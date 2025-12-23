package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DiagnosticsPayloadBuilder {

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    public static JSONObject build(Context context, List<DiagnosticResult> results) {

        JSONObject payload = new JSONObject();

        try {
            String id = getAppInstanceId(context);
            
            // Map the unique ID to both deviceId and terminal_id to satisfy the backend
            payload.put("deviceId", id);
            payload.put("terminal_id", id);
            
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("model", Build.MODEL);
            payload.put("androidVersion", Build.VERSION.RELEASE);
            payload.put("sdkLevel", Build.VERSION.SDK_INT);
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
            // Never crash
        }

        return payload;
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    private synchronized static String getAppInstanceId(Context context) {
        if (uniqueID == null) {
            android.content.SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                sharedPrefs.edit().putString(PREF_UNIQUE_ID, uniqueID).apply();
            }
        }
        return uniqueID;
    }
}
