package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.os.Build;
import java.util.UUID;

public class DeviceInfoTestV2 implements DiagnosticTest {

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    @Override
    public String getName() {
        return "Device Info";
    }

    @Override
    public DiagnosticResult run(Context context) {
        String details = "Manufacturer: " + Build.MANUFACTURER + "\n" +
                "Model: " + Build.MODEL + "\n" +
                "Android Version: " + Build.VERSION.RELEASE + "\n" +
                "SDK Level: " + Build.VERSION.SDK_INT + "\n" +
                "App Instance ID: " + getAppInstanceId(context);

        return new DiagnosticResult(
                getName(),
                DiagnosticStatus.PASS,
                details
        );
    }

    private synchronized String getAppInstanceId(Context context) {
        if (uniqueID == null) {
            android.content.SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                android.content.SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.apply();
            }
        }
        return uniqueID;
    }
}
