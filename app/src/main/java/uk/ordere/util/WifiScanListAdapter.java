package uk.ordere.util;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ordere.launcher.R;

import static uk.ordere.util.NetworkCapability.isEncryptionEnabledSupported;

public class WifiScanListAdapter {
    private boolean _connectedNetworkSecure = false;
    private ArrayList<Object> _dataset = new ArrayList<>();
    private ArrayAdapter<Object> _arrayAdapter;
    private HashMap<String, WifiConfiguration> _savedNetworks = new HashMap<>();

    public enum WifiNetworkState {CONNECTED, SAVED, UNSUPPORTED, HIDDEN, NONE};

    public class WifiBriefInfo {
        public static final int NO_SIGNAL_RSSI = -110;

        public final String ssid;
        public final boolean secure;
        public final int rssi;
        public final WifiNetworkState state;
        public final SupplicantState suplState;
        public final Integer networkId;

        public WifiBriefInfo(String ssid, boolean secure, int rssi, WifiNetworkState state, SupplicantState suplState, Integer networkId) {
            this.ssid = ssid;
            this.secure = secure;
            this.rssi = rssi;
            this.state = state;
            this.suplState = suplState;
            this.networkId = networkId;
        }
    }

    private class MyArrayAdapter extends ArrayAdapter<Object> {
        private LightingColorFilter _colorFilterConnected;
        private LightingColorFilter _colorFilterNormal;
        private LightingColorFilter _colorFilterDisabled;

        public MyArrayAdapter(Context ctx, int resource, List<Object> objects) {
            super(ctx, resource, objects);
            _colorFilterConnected = new LightingColorFilter(0, ctx.getResources().getColor(R.color.colorPrimary));
            _colorFilterNormal = new LightingColorFilter(0, ctx.getResources().getColor(R.color.textPrimary));
            _colorFilterDisabled = new LightingColorFilter(0, ctx.getResources().getColor(R.color.textDisabled));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView != null
                    ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.wifi_scan_item, parent, false);

            // controls
            ImageView iconView = itemView.findViewById(R.id.wifi_scan_item_icon);
            TextView ssidView = itemView.findViewById(R.id.wifi_scan_item_ssid);

            // tightly coupled, but doesn't matter as I'm an inner class anyway :)
            WifiBriefInfo info = getBriefInfo(position);

            switch (info.state) {
                case CONNECTED:
                    iconView.setImageResource(getIconResourceFromRSSI(info.rssi, info.secure, false));
                    iconView.setColorFilter(_colorFilterConnected);
                    showStatus(itemView, translateSuplState(info.suplState).toString());
                    break;
                case NONE:
                    iconView.setImageResource(getIconResourceFromRSSI(info.rssi, info.secure, false));
                    iconView.setColorFilter(_colorFilterNormal);
                    showStatus(itemView, "");
                    break;
                case SAVED:
                    iconView.setImageResource(getIconResourceFromRSSI(info.rssi, info.secure, false));
                    iconView.setColorFilter(_colorFilterNormal);
                    showStatus(itemView, R.string.wifi_has_profile);
                    break;
                case HIDDEN:
                case UNSUPPORTED:
                    iconView.setImageResource(getIconResourceFromRSSI(info.rssi, info.secure, true));
                    iconView.setColorFilter(_colorFilterDisabled);
                    showStatus(itemView, R.string.wifi_unsupported);
                    break;
            }

            if(info.ssid.isEmpty())
                ssidView.setText(R.string.wifi_ssid_hidden);
            else
                ssidView.setText(info.ssid);

            return itemView;
        }

        private void showStatus(View itemView, int rsc) {
            showStatus(itemView, getContext().getResources().getString(rsc));
        }

        private void showStatus(View itemView, String msg) {
            TextView statusSubview = itemView.findViewById(R.id.wifi_scan_item_info);
            if(msg.isEmpty()) {
                statusSubview.setVisibility(View.GONE);
            } else {
                statusSubview.setVisibility(View.VISIBLE);
                statusSubview.setText(msg);
            }
        }

        private int getIconResourceFromRSSI(int rssi, boolean secure, boolean unsupported) {
            if(unsupported)
                return R.drawable.baseline_wifi_off_24;
            switch(WifiManager.calculateSignalLevel(rssi, 4)) {
                case 0:
                    return secure ? R.drawable.baseline_signal_wifi_1_bar_lock_24 : R.drawable.baseline_signal_wifi_1_bar_24;
                case 1:
                    return secure ? R.drawable.baseline_signal_wifi_2_bar_lock_24 : R.drawable.baseline_signal_wifi_2_bar_24;
                case 2:
                    return secure ? R.drawable.baseline_signal_wifi_3_bar_lock_24 : R.drawable.baseline_signal_wifi_3_bar_24;
                case 3:
                    return secure ? R.drawable.baseline_signal_wifi_4_bar_lock_24 : R.drawable.baseline_signal_wifi_4_bar_24;
                default:
                    return R.drawable.baseline_wifi_off_24;
            }
        }

        private CharSequence translateSuplState(SupplicantState state) {
            int strRes;
            switch (state) {
                case COMPLETED:
                    strRes = R.string.supl_state_completed;
                    break;
                case FOUR_WAY_HANDSHAKE:
                case GROUP_HANDSHAKE:
                case AUTHENTICATING:
                    strRes = R.string.supl_state_authenticating;
                    break;
                case ASSOCIATED:
                    strRes = R.string.supl_state_associated;
                    break;
                case ASSOCIATING:
                    strRes = R.string.supl_state_associating;
                    break;
                default:
                    strRes = R.string.supl_state_other;
                    break;
            }
            return getContext().getText(strRes);
        }
    }

    private String unquoteConnectedSSID(String ssid) {
        return ssid.startsWith("\"") ? ssid.substring(1, ssid.length() - 1) : ssid;
    }

    private void updateDataset(List<ScanResult> scannedList, WifiInfo connected) {
        // using null as an argument to both parameters will clear the ListView!
        // only scannedList is null, we update the connected network info only

        if(scannedList == null && connected == null) {
            // clear only
            _dataset.clear();
            return;
        }

        String connectedSSID = null;
        if(connected != null && connected.getBSSID() != null && !connected.getBSSID().equals("00:00:00:00:00:00")) {
            // Interpret this network as connected only for a a certain supplicant state.
            // Other states indicates disconnected or other errors, such as interface or supplicant errors.
            switch (connected.getSupplicantState()) {
                case COMPLETED:
                case ASSOCIATED:
                case ASSOCIATING:
                case AUTHENTICATING:
                case FOUR_WAY_HANDSHAKE:
                case GROUP_HANDSHAKE:
                    connectedSSID = unquoteConnectedSSID(connected.getSSID());
                    break;
            }
        }

        if (connectedSSID != null && scannedList != null) {
            _dataset.clear();
            _dataset.add(connected);
            for(ScanResult result : scannedList) {
                if(result.SSID.equals(connectedSSID)) {
                    _connectedNetworkSecure = isEncryptionEnabledSupported(result.capabilities).first;
                } else {
                    _dataset.add(result);
                }
            }
        } else if(connectedSSID != null && _dataset.size() >= 1) {
            if(_dataset.get(0) instanceof WifiInfo) {
                // this means that previous dataset already has connection info.
                // here we only update connected network's status

                _dataset.set(0, connected);
            } else {
                // here the dataset does not have previous connection info. perform rescan!
                ArrayList<ScanResult> tmpDataset = new ArrayList<>();
                for(Object exRs : _dataset) {
                    ScanResult exRsC = (ScanResult)exRs;
                    if(exRsC.SSID.equals(connectedSSID)) {
                        _connectedNetworkSecure = isEncryptionEnabledSupported(exRsC.capabilities).first;
                    } else {
                        tmpDataset.add(exRsC);
                    }
                }
                _dataset.clear();
                _dataset.add(connected);
                _dataset.addAll(tmpDataset);
            }
        } else if (scannedList != null) {
            // here we assume that the system is not connected to any network
            _dataset.clear();
            _dataset.addAll(scannedList);
        }
    }

    public ArrayAdapter getArrayAdapter() {
        return _arrayAdapter;
    }

    public ArrayList<Object> getArrayList() {
        return _dataset;
    }

    public WifiScanListAdapter(Context ctx) {
        _arrayAdapter = new MyArrayAdapter(ctx, 0, _dataset);
        _arrayAdapter.setNotifyOnChange(false);
    }

    public void updateData(List<ScanResult> scannedList, WifiInfo connected) {
        updateDataset(scannedList, connected);
        _arrayAdapter.notifyDataSetChanged();
    }

    public void updateSavedNetworks(List<WifiConfiguration> savedNets) {
        _savedNetworks.clear();
        for(WifiConfiguration cfg : savedNets) {
            _savedNetworks.put(unquoteConnectedSSID(cfg.SSID), cfg);
        }
        _arrayAdapter.notifyDataSetChanged();
    }

    public WifiBriefInfo getBriefInfo(int position) {
        Object item = _arrayAdapter.getItem(position);
        if(item instanceof WifiInfo) {
            // connected Wi-Fi
            WifiInfo info = (WifiInfo)item;
            String ssid = unquoteConnectedSSID(info.getSSID());
            Integer netId = _savedNetworks.containsKey(ssid) ? _savedNetworks.get(ssid).networkId : null;
            return new WifiBriefInfo(ssid, _connectedNetworkSecure, info.getRssi(), WifiNetworkState.CONNECTED, info.getSupplicantState(), netId);
        } else {
            // unconnected Wi-Fi. If saved, we're ready to connect.
            // otherwise, we only support non hidden network and WPA/WPA2 with PSK passphrase.
            ScanResult result = (ScanResult)item;
            Pair<Boolean, Boolean> netStatus = isEncryptionEnabledSupported(result.capabilities);
            if(_savedNetworks.containsKey(result.SSID)) {
                return new WifiBriefInfo(result.SSID, netStatus.first, result.level,
                        WifiNetworkState.SAVED, null, _savedNetworks.get(result.SSID).networkId);
            } else {
                boolean hidden = result.SSID.isEmpty();
                return new WifiBriefInfo(result.SSID, netStatus.first, result.level,
                        hidden ? WifiNetworkState.HIDDEN : (netStatus.second ? WifiNetworkState.NONE : WifiNetworkState.UNSUPPORTED),
                        null, null);
            }
        }
    }
}
