package com.nesty.pos.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "nesty_pos_session";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_OPERATOR_ID = "operator_id";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_OPERATOR_ROLE = "operator_role";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_TERMINAL_ID = "terminal_id";
    private static final String KEY_DEVICE_ID = "pos_device_id";
    private static final String KEY_DEVICE_KEY = "pos_device_key";

    public static final String DEFAULT_BASE_URL = "https://nesty-service-app.onrender.com/";
    public static final String DEFAULT_TERMINAL_ID = "POS-1008-Counter1";
    public static final String DEFAULT_DEVICE_ID = "POS-1008-COUNTER1";
    public static final String DEFAULT_DEVICE_KEY = "nesty_pos_dev_sec_7a8f9c1e2b4d";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String operatorId, String operatorName, String role) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_OPERATOR_ID, operatorId)
            .putString(KEY_OPERATOR_NAME, operatorName)
            .putString(KEY_OPERATOR_ROLE, role)
            .apply();
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public String getOperatorName() {
        return prefs.getString(KEY_OPERATOR_NAME, "Counter Operator");
    }

    public String getOperatorId() {
        return prefs.getString(KEY_OPERATOR_ID, "POS-1008");
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    public void setBaseUrl(String url) {
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }

    public String getTerminalId() {
        return prefs.getString(KEY_TERMINAL_ID, DEFAULT_TERMINAL_ID);
    }

    public void setTerminalId(String terminalId) {
        prefs.edit().putString(KEY_TERMINAL_ID, terminalId).apply();
    }

    public String getDeviceId() {
        return prefs.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID);
    }

    public void setDeviceId(String deviceId) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }

    public String getDeviceKey() {
        return prefs.getString(KEY_DEVICE_KEY, DEFAULT_DEVICE_KEY);
    }

    public void setDeviceKey(String deviceKey) {
        prefs.edit().putString(KEY_DEVICE_KEY, deviceKey).apply();
    }

    public void clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_OPERATOR_ID)
            .remove(KEY_OPERATOR_NAME)
            .remove(KEY_OPERATOR_ROLE)
            .apply();
    }
}

