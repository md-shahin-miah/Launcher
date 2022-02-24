package uk.ordere.launcher.wifisettings;


import android.util.Pair;

public final class NetworkCapability {
    public static final String WPA2 = "WPA2";
    public static final String WPA = "WPA";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";
    public static final String WPA_EAP = "WPA-EAP";
    public static final String IEEE8021X = "IEEE8021X";
    public static final String ENTERPRISE_CAPABILITY = "-EAP-";
    public static final String PRE_SHARED_KEY = "PSK";
    public static final String ADHOC = "[IBSS]";

    private NetworkCapability(){}

    public static boolean isAdhoc(String cap) {
        return cap.contains(ADHOC);
    }

    public static boolean isEnterprise(String cap) {
        return cap.contains(ENTERPRISE_CAPABILITY);
    }

    // F = encryption enabled, S = encryption supported
    public static Pair<Boolean, Boolean> isEncryptionEnabledSupported(String cap) {
        final String[] securityModesUnsup = { WEP, WPA_EAP, IEEE8021X };
        final String[] securityModesSup = { WPA, WPA2 };
        for(String s : securityModesUnsup) {
            if(cap.contains(s)) {
                return new Pair<>(true, false);
            }
        }
        // must not enterprise / EAP
        if(isEnterprise(cap)) {
            return new Pair<>(true, false);
        }
        // check whether supported encryption is enabled
        boolean secure = false;
        for(String s : securityModesSup) {
            if(cap.contains(s)) {
                secure = true;
                break;
            }
        }
        // yes we support this if this is not an adhoc!
        return new Pair<>(secure, !isAdhoc(cap));
    }
}