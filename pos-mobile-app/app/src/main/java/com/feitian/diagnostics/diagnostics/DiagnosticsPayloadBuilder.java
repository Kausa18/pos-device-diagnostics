package com.feitian.diagnostics.diagnostics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiagnosticsPayloadBuilder {

    private static final String TAG = "DiagnosticsPayload";

    public static JSONObject build(Context context, List<DiagnosticResult> results) {

        JSONObject payload = new JSONObject();

        try {
            // Get Serial Number (Terminal ID)
            String serial = getDeviceSerialNumber(context);
            
            payload.put("terminal_id", serial);
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("model", Build.MODEL);
            payload.put("androidVersion", Build.VERSION.RELEASE);
            payload.put("timestamp", getTimestamp());

            JSONArray resultsArray = new JSONArray();

            for (DiagnosticResult result : results) {
                JSONObject item = new JSONObject();
                item.put("name", result.getName());
                item.put("status", result.getStatus().name());
                item.put("details", result.getDetails());
                resultsArray.put(item);
            }

            payload.put("results", resultsArray);

        } catch (Exception e) {
            Log.e(TAG, "Error building JSON", e);
        }

        return payload;
    }

    private static String getTimestamp() {
        // Using Locale.US for consistent timestamp formatting across all devices
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    @SuppressLint("HardwareIds")
    private static String getDeviceSerialNumber(Context context) {
        String serial = null;

        // 1. Try Feitian SDK Hardware Serial (Highest Priority for POS)
        try {
            com.ftpos.library.smartpos.device.Device device = com.ftpos.library.smartpos.device.Device.getInstance(context);
            if (device != null) {
                serial = device.getSerialNumber();
            }
        } catch (Throwable ignored) {}

        // 2. Fallback to Android Secure ID
        // Note: Using ANDROID_ID is discouraged for advertising but acceptable for 
        // internal hardware diagnostics when privileged serial access is denied.
        if (serial == null || serial.isEmpty() || serial.equalsIgnoreCase("unknown")) {
            try {
                serial = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) {}
        }

        return (serial == null || serial.isEmpty()) ? "SN-UNAVAILABLE" : serial;
    }
}
