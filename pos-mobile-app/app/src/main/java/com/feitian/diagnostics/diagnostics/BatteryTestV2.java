package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Battery Health";
    }

    @Override
    public DiagnosticResult run(Context context) {
        DiagnosticStatus status;
        String details = "";

        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);

            if (batteryStatus == null) {
                return new DiagnosticResult(
                        getName(),
                        DiagnosticStatus.SKIPPED,
                        "Battery information unavailable"
                );
            }

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

            int batteryPct = (int) ((level / (float) scale) * 100);

            details += "Level: " + batteryPct + "%\n";
            details += "Temperature: " + (temperature / 10.0) + "°C\n";
            details += "Voltage: " + voltage + "mV\n";

            String healthLabel;
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    healthLabel = "Good";
                    status = DiagnosticStatus.PASS;
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthLabel = "Overheat";
                    status = DiagnosticStatus.FAIL;
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    healthLabel = "Dead";
                    status = DiagnosticStatus.FAIL;
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    healthLabel = "Over Voltage";
                    status = DiagnosticStatus.FAIL;
                    break;
                default:
                    healthLabel = "Unknown";
                    status = DiagnosticStatus.SKIPPED;
            }

            details += "Health: " + healthLabel;

        } catch (Exception e) {
            status = DiagnosticStatus.FAIL;
            details += "Error reading battery info: " + e.getMessage();
        }

        return new DiagnosticResult(getName(), status, details);
    }
}
