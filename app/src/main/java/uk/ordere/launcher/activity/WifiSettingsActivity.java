package uk.ordere.launcher.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import uk.ordere.launcher.R;
import uk.ordere.util.WifiScanListAdapter;

import static android.content.ContentValues.TAG;

public class WifiSettingsActivity extends Activity
        implements CompoundButton.OnCheckedChangeListener, ListView.OnItemClickListener {

    //constants
    private static final int _wifiRescanIntervalMs = 1000;
    private static final int _wifiRequestScanIntervalCount = 5;
    private static final int _permRequestCode = 123;
    private static final String _logTag = "MainActivity";

    private boolean _handleMainSwitchToggleChange = false;
    private boolean _periodicWifiScanActive = false;
    private int _wifiRequestScanIntervalCounter = 0;
    private HashSet<String> _grantedPermissions = new HashSet<>();
    private WifiManager _wifiManager;
    private Handler _wifiRescanHandler = new Handler();
    private WifiScanListAdapter _wifiScanListAdapter;

    // To keep track of activity's window focus
    boolean currentFocus;

    // To keep track of activity's foreground/background status
    boolean isPaused;

    Handler collapseNotificationHandler;


    // subviews in this views
    private Switch _mainWifiSwitch;
    private ListView _mainWifiList;
    private  TextView waitingText;

    private abstract class WifiDialogActionListener implements DialogInterface.OnClickListener {
        public WifiScanListAdapter.WifiBriefInfo networkInfo;
    }



    private Runnable _wifiRescanRunnable = new Runnable() {
        @Override
        public void run() {
            // update current connection info
            _wifiScanListAdapter.updateData(null, _wifiManager.getConnectionInfo());
            // request OS for rescan
            if (_wifiRequestScanIntervalCounter++ >= _wifiRequestScanIntervalCount) {
                _wifiRequestScanIntervalCounter = 0;
                if ((_grantedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION") && _grantedPermissions.contains("android.permission.ACCESS_WIFI_STATE"))) {
                    Log.i(_logTag, "Wi-Fi scan initialized");
                    if (!_wifiManager.startScan()) {
//                        Toast.makeText(MainActivity.this, getText(R.string.msg_wifi_scan_failed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(_logTag, "Please grant ACCESS_COARSE_LOCATION and ACCESS_WIFI_STATE to scan Wi-Fi.");
                }
            }
            // run again after defined time
            if (_periodicWifiScanActive)
                _wifiRescanHandler.postDelayed(_wifiRescanRunnable, _wifiRescanIntervalMs);


            if (_wifiScanListAdapter.getArrayList().size()==0){

                waitingText.setVisibility(View.VISIBLE);


            }
            else{
                waitingText.setVisibility(View.GONE);
            }

        }
    };

    private BroadcastReceiver _myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNetworkList();
        }
    };

    private WifiDialogActionListener _connectWifiDialogListener = new WifiDialogActionListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            _wifiManager.disconnect();
            int msgRsc = R.string.action_result_connect;
            String passphrase = ((EditText) ((Dialog) dialog).findViewById(R.id.dlg_wifi_act_passphrase_input)).getText().toString();
            if (networkInfo.networkId != null) {
                // we simply connect to the saved network
                forceWifiRescan(1500);
                _wifiManager.enableNetwork(networkInfo.networkId, true);

            } else if (networkInfo.secure && passphrase.length() < 8) {
                // an invalid condition as we only supports WPA PSK
                msgRsc = R.string.action_result_invalid_passphrase;
            } else {
                // create new profile and connect to it
                WifiConfiguration cfg = new WifiConfiguration();
                cfg.SSID = "\"" + networkInfo.ssid + "\"";
                if (networkInfo.secure) {
                    cfg.preSharedKey = "\"" + passphrase + "\"";
                } else {
                    cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                int netId = _wifiManager.addNetwork(cfg);
                if (netId < 0) {
                    //profile creation failed
                    msgRsc = R.string.action_result_create_profile_error;
                } else {
                    if (ActivityCompat.checkSelfPermission(WifiSettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    _wifiScanListAdapter.updateSavedNetworks(_wifiManager.getConfiguredNetworks());
                    forceWifiRescan(1500);
                    _wifiManager.enableNetwork(netId, true);
                }
            }
            Toast.makeText(WifiSettingsActivity.this, msgRsc, Toast.LENGTH_LONG).show();
        }
    };

    private WifiDialogActionListener _forgetWifiDialogListener = new WifiDialogActionListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            boolean status = _wifiManager.removeNetwork(networkInfo.networkId);
            Toast.makeText(WifiSettingsActivity.this,
                    status ? R.string.action_result_profile_delete : R.string.action_result_profile_cannot_delete,
                    Toast.LENGTH_LONG).show();
            if (status) {
                if (ActivityCompat.checkSelfPermission(WifiSettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                _wifiScanListAdapter.updateSavedNetworks(_wifiManager.getConfiguredNetworks());
                forceWifiRescan(500);
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        currentFocus = hasFocus;

        if (!hasFocus) {

            // Method that handles loss of window focus
            collapseNow();
        }
    }

    public void collapseNow() {

        // Initialize 'collapseNotificationHandler'
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = new Handler();
        }

        // If window focus has been lost && activity is not in a paused state
        // Its a valid check because showing of notification panel
        // steals the focus from current activity's window, but does not
        // 'pause' the activity
        if (!currentFocus && !isPaused) {

            // Post a Runnable with some delay - currently set to 300 ms
            collapseNotificationHandler.postDelayed(new Runnable() {

                @Override
                public void run() {

                    // Use reflection to trigger a method from 'StatusBarManager'

                    @SuppressLint("WrongConstant") Object statusBarService = getSystemService("statusbar");
                    Class<?> statusBarManager = null;

                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    Method collapseStatusBar = null;

                    try {

                        // Prior to API 17, the method to call is 'collapse()'
                        // API 17 onwards, the method to call is `collapsePanels()`

                        if (Build.VERSION.SDK_INT > 16) {
                            collapseStatusBar = statusBarManager .getMethod("collapsePanels");
                        } else {
                            collapseStatusBar = statusBarManager .getMethod("collapse");
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    collapseStatusBar.setAccessible(true);

                    try {
                        collapseStatusBar.invoke(statusBarService);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    // Check if the window focus has been returned
                    // If it hasn't been returned, post this Runnable again
                    // Currently, the delay is 100 ms. You can change this
                    // value to suit your needs.
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler.postDelayed(this, 100L);
                    }

                }
            }, 300L);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);

        _wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        _wifiScanListAdapter = new WifiScanListAdapter(this);

        //populate subviews
        _mainWifiSwitch = findViewById(R.id.main_wifi_switch);
        _mainWifiList = findViewById(R.id.main_wifi_list);
        waitingText=findViewById(R.id.waitingText);

        //event handlers
        _mainWifiSwitch.setOnCheckedChangeListener(this);
        _mainWifiList.setOnItemClickListener(this);



        _mainWifiList.setAdapter(_wifiScanListAdapter.getArrayAdapter());




        try {
            checkRequiredPermissions();
        } catch (Exception e) {
            Log.d(TAG, "onCreate: " + e);
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.main_wifi_switch: {
                if (_handleMainSwitchToggleChange) {
                    _wifiManager.setWifiEnabled(isChecked);
                    setPeriodicWifiScanActive(isChecked);
                }
                buttonView.setText(getString(R.string.wifi_switch, getString(isChecked ? R.string.wifi_on : R.string.wifi_off)));
                if (!isChecked) {
                    _wifiScanListAdapter.updateData(null, null);
                }
                break;
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        WifiScanListAdapter.WifiBriefInfo networkInfo = _wifiScanListAdapter.getBriefInfo(position);
        if (networkInfo == null) return;
        AlertDialog.Builder bld = new AlertDialog.Builder(this);

        switch (networkInfo.state) {
            case HIDDEN:
            case UNSUPPORTED:
                bld.setTitle(R.string.dlg_wifi_unsupported_title);
                bld.setMessage(networkInfo.state == WifiScanListAdapter.WifiNetworkState.HIDDEN
                        ? R.string.dlg_warn_hidden
                        : R.string.dlg_warn_unsupported);
                bld.setNegativeButton(R.string.dlg_action_dismiss, null);
                break;
            default: {
                // dialog title
                switch (networkInfo.state) {
                    case SAVED:
                        bld.setTitle(R.string.dlg_wifi_saved_title);
                        break;
                    case CONNECTED:
                        bld.setTitle(R.string.dlg_wifi_connected_title);
                        break;
                    default:
                        bld.setTitle(R.string.dlg_wifi_connect_title);
                        break;
                }
                // dialog contents
                View dlgView = getLayoutInflater().inflate(R.layout.wifi_action_dialog, null, false);
                EditText passphraseInput = dlgView.findViewById(R.id.dlg_wifi_act_passphrase_input);
                bld.setView(dlgView);
                ((TextView) dlgView.findViewById(R.id.dlg_wifi_act_ssid_input)).setText(networkInfo.ssid);
                ((TextView) dlgView.findViewById(R.id.dlg_wifi_act_security_input)).setText(
                        networkInfo.secure ? R.string.wifi_secure : R.string.wifi_open);
                if (!networkInfo.secure) {
                    dlgView.findViewById(R.id.dlg_wifi_act_passphrase_caption).setVisibility(View.GONE);
                    passphraseInput.setVisibility(View.GONE);
                }
                _connectWifiDialogListener.networkInfo = _forgetWifiDialogListener.networkInfo = networkInfo;
                switch (networkInfo.state) {
                    case SAVED:
                        //action:connect
                        bld.setPositiveButton(R.string.dlg_action_connect, _connectWifiDialogListener);
                    case CONNECTED:
                        //user cannot edit/view saved passwords
                        passphraseInput.setText(R.string.dlg_passphrase_saved);
                        passphraseInput.setFocusable(false);
                        passphraseInput.setFocusableInTouchMode(false);
                        passphraseInput.setClickable(false);
                        //action:forget
                        bld.setNeutralButton(R.string.dlg_action_forget, _forgetWifiDialogListener);
                        break;
                    case NONE:
                        // this marks that we're not connecting from any existing profiles
                        bld.setPositiveButton(R.string.dlg_action_connect, _connectWifiDialogListener);
                }
                break;
            }
        }

        bld.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        // trigger text refresh
        _mainWifiSwitch.setChecked(true);
        // set initial button state to actual Wi-Fi state
        _mainWifiSwitch.setChecked(_wifiManager.isWifiEnabled());
        // reassign event handlers for our main switch
        _handleMainSwitchToggleChange = true;

        // get saved network profiles
        if (ActivityCompat.checkSelfPermission(WifiSettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        _wifiScanListAdapter.updateSavedNetworks(_wifiManager.getConfiguredNetworks());

        //activate periodic wifi scan if wifi is active
        setPeriodicWifiScanActive(_wifiManager.isWifiEnabled());
    }


    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        _handleMainSwitchToggleChange = false;

        //deactivate periodic wifi scan
        setPeriodicWifiScanActive(false);
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == _permRequestCode) {
            for(int i=0; i<permissions.length; ++i) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    _grantedPermissions.add(permissions[i]);
                }
            }
        }
    }

    private void setPeriodicWifiScanActive(boolean active) {
        //make sure we don't doubly activate/deactivate running periodic handler
        if(active == _periodicWifiScanActive) {
            return;
        }
        if(active) {
            _wifiRescanHandler.postDelayed(_wifiRescanRunnable, 0);
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(_myReceiver, filter);
        } else {
            _wifiRescanHandler.removeCallbacks(_wifiRescanRunnable);
            unregisterReceiver(_myReceiver);
        }
        _periodicWifiScanActive = active;
    }

    private void forceWifiRescan(int delayMs) {
        _wifiRescanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // update only with last scan results
                _wifiScanListAdapter.updateData(_wifiManager.getScanResults(), _wifiManager.getConnectionInfo());
            }
        }, delayMs);
    }

    private void checkRequiredPermissions() {
        if(Build.VERSION.SDK_INT < 23) {
            return;
        }
        PackageInfo myInfo;
        try {
            myInfo = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (Exception ex) {
            return;
        }
        //check if all required permissions are granted
        ArrayList<String> deniedPermissions = new ArrayList<>();
        for(String perm : myInfo.requestedPermissions) {
            if(checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                _grantedPermissions.add(perm);
            } else {
                deniedPermissions.add(perm);
            }
        }
        //req!
        requestPermissions(deniedPermissions.toArray(new String[0]), _permRequestCode);
    }

    private void refreshNetworkList() {
        List<ScanResult> scanResults = _wifiManager.getScanResults();
        WifiInfo info = _wifiManager.getConnectionInfo();
        _wifiScanListAdapter.updateData(scanResults, info);
    }

}
