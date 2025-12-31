package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;

public class ChargingPortTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Charging Port Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        // Since this is called from a background thread in DiagnosticRunner,
        // we poll for the charging status for a limited time.
        
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds timeout

        try {
            while (System.currentTimeMillis() - startTime < timeout) {
                if (isPluggedIn(context)) {
                    return new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Charging detected successfully.");
                }
                //noinspection BusyWait
                Thread.sleep(1000); // Check every second
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "Test interrupted.");
        }

        return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "No charger detected within 10 seconds.");
    }

    private boolean isPluggedIn(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return chargePlug == BatteryManager.BATTERY_PLUGGED_USB || 
               chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
               chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    public static String getPrompt() {
        return "\nCharging Port Test:\nPlease connect your charger now (10s timeout)...\n";
    }
}
