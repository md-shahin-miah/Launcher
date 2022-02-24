package uk.ordere.launcher.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import uk.ordere.launcher.R;
import uk.ordere.launcher.bluetooth.BluetoothMainActivity;
import uk.ordere.util.MyPref;

public class SettingActivity extends AppCompatActivity {

    RelativeLayout layoutBrightness;
    Slider slider;
    MyPref myPref;
    // To keep track of activity's window focus
    boolean currentFocus;

    // To keep track of activity's foreground/background status
    boolean isPaused;

    Handler collapseNotificationHandler;


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


    public static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, HomeActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String currentHomePackage = resolveInfo.activityInfo.packageName;

        Log.e("DATA_PAGE",currentHomePackage);
        resetPreferredLauncherAndOpenChooser(SettingActivity.this);
//        myPref = MyPref.getInstance(getApplicationContext());
//        layoutBrightness = findViewById(R.id.layout_brightness);
//        slider = findViewById(R.id.brightness_slider);
//
//        int brightness = myPref.getBrightness();
//        slider.setValue((float)brightness);
//        layoutBrightness.setOnClickListener(view -> {
//
//            if(findViewById(R.id.brightness_arrow_up).getVisibility()== View.VISIBLE){
//                findViewById(R.id.brightness_arrow_up).setVisibility(View.GONE);
//                findViewById(R.id.brightness_arrow_down).setVisibility(View.VISIBLE);
//                slider.setVisibility(View.GONE);
//            }else{
//                findViewById(R.id.brightness_arrow_down).setVisibility(View.GONE);
//                findViewById(R.id.brightness_arrow_up).setVisibility(View.VISIBLE);
//                slider.setVisibility(View.VISIBLE);
//
//            }
//
//        });

        findViewById(R.id.layout_wifi).setOnClickListener(view -> startActivity(new Intent(SettingActivity.this,WifiSettingsActivity.class)));

        findViewById(R.id.layout_bluetooth).setOnClickListener(view -> startActivity(new Intent(SettingActivity.this, BluetoothMainActivity.class)));

        findViewById(R.id.system_setting).setOnClickListener(view -> {
            Intent intent1 = new Intent(SettingActivity.this,PinCodeActivity.class);
            intent1.putExtra("route","setting");
            startActivity(intent1);
        });
        findViewById(R.id.change_launcher).setOnClickListener(view -> {
            Intent intent1 = new Intent(SettingActivity.this,PinCodeActivity.class);
            intent1.putExtra("route","default_launcher");
            startActivity(intent1);
        });




//        slider.addOnChangeListener((slider1, value, fromUser) -> {
//            // Check whether has the write settings permission or not.
//            boolean settingsCanWrite;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                settingsCanWrite = Settings.System.canWrite(getApplicationContext());
//
//                if (!settingsCanWrite) {
//                    // If do not have write settings permission then open the Can modify system settings panel.
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                    startActivity(intent);
//                } else {
//                    setBrightness((int) value);
//
//                }
//            }
//        });


    }


//    public void setBrightness(int brightness){
//
//        //constrain the value of brightness
//        if(brightness < 0)
//            brightness = 0;
//        else if(brightness > 255)
//            brightness = 255;
//
//
//        ContentResolver cResolver = this.getApplicationContext().getContentResolver();
//        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
//
//        myPref.saveBrightness(brightness);
//
//    }

    @Override
    protected void onPause() {
        // Activity's been paused
        isPaused = true;
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activity's been paused
        isPaused = false;
    }
}