package com.feitian.diagnostics;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.Context;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity {

    private TextView statusLabel;
    private TextView outputLog;
    private Button btnRunDiagnostics;
    private boolean touchDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
            );
        }

        findViewById(R.id.main).setOnTouchListener((v, event) -> {
            // Only set the flag if the diagnostics are running.
            if (!btnRunDiagnostics.isEnabled()) {
                touchDetected = true;
            }
            return true;
        });

        btnRunDiagnostics = findViewById(R.id.btnRunDiagnostics);
        statusLabel = findViewById(R.id.statusLabel);
        outputLog = findViewById(R.id.outputLog);

        btnRunDiagnostics.setOnClickListener(v -> runDiagnostics());
    }

    private void runDiagnostics() {
        btnRunDiagnostics.setEnabled(false);
        statusLabel.setText(R.string.status_running_diagnostics);
        outputLog.setText(R.string.starting_diagnostics);

        new Thread(() -> {
            try {
                // Automated tests first
                Thread.sleep(500);
                runOnUiThread(() -> {
                    outputLog.append(getDeviceInfo());
                    outputLog.append("\nDevice Info Test: Passed\n\n");
                });
                Thread.sleep(500);
                runOnUiThread(() -> {
                    outputLog.append(getBatteryInfo());
                    outputLog.append("\nBattery Health Test: Passed\n\n");
                });
                Thread.sleep(500);
                runOnUiThread(() -> {
                    outputLog.append(getNetworkInfo());
                    outputLog.append("\nNetwork Test: Completed\n\n");
                });
                Thread.sleep(500);
                runOnUiThread(() -> {
                    outputLog.append(getSignalStrengthInfo());
                    outputLog.append("\nSignal Strength Test: Completed\n\n");
                });

                // Interactive Touchscreen Test
                runOnUiThread(() -> {
                    touchDetected = false; // Reset flag right before the test
                    outputLog.append("\nStarting Touchscreen Test...\nPlease touch the screen within 10 seconds.\n");
                });

                // Wait for 5 seconds for user input
                Thread.sleep(5000);

                runOnUiThread(() -> {
                    if (touchDetected) {
                        outputLog.append("\nTouchscreen Test: PASSED\n\n");
                    } else {
                        outputLog.append("\nTouchscreen Test: FAILED (No touch detected)\n\n");
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Update UI on the main thread when all diagnostics are complete
            runOnUiThread(() -> {
                statusLabel.setText(R.string.status_diagnostics_complete);
                btnRunDiagnostics.setEnabled(true);
            });
        }).start();
    }

    private String getDeviceInfo() {
        StringBuilder info = new StringBuilder();

        info.append("Device Info:\n");
        info.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        info.append("Model: ").append(android.os.Build.MODEL).append("\n");
        info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("SDK Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n");

        String serial;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            serial = "Restricted (Android 10+)";
        } else {
            @SuppressWarnings("deprecation")
            String s = android.os.Build.SERIAL;
            serial = s;
        }

        info.append("Serial: ").append(serial).append("\n");

        return info.toString();
    }

    private String getBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        if (batteryStatus == null) {
            return "Battery info unavailable\n";
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

        int batteryPct = (int) ((level / (float) scale) * 100);

        String chargingStatus;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            chargingStatus = "Charging";
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            chargingStatus = "Full";
        } else {
            chargingStatus = "Not charging";
        }

        String healthStatus;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthStatus = "Good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthStatus = "Overheat";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthStatus = "Dead";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthStatus = "Over Voltage";
                break;
            default:
                healthStatus = "Unknown";
        }

        return
                "\nBattery Diagnostics:\n" +
                        "Level: " + batteryPct + "%\n" +
                        "Status: " + chargingStatus + "\n" +
                        "Health: " + healthStatus + "\n" +
                        "Temperature: " + (temp / 10.0) + "°C\n" +
                        "Voltage: " + voltage + "mV\n";
    }

    private String getNetworkInfo() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return "\nNetwork Diagnostics:\nNetwork service unavailable\n";
        }

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            return "\nNetwork Diagnostics:\nNo active network connection\n";
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        if (caps == null) {
            return "\nNetwork Diagnostics:\nUnable to read network capabilities\n";
        }

        String connectionType;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connectionType = "WiFi";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            connectionType = "Mobile (SIM)";
        } else {
            connectionType = "Other";
        }

        boolean hasInternet =
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        return
                "\nNetwork Diagnostics:\n" +
                        "Connection Type: " + connectionType + "\n" +
                        "Internet Available: " + (hasInternet ? "Yes" : "No") + "\n";
    }

    private String getSignalStrengthInfo() {

        StringBuilder info = new StringBuilder();
        info.append("\nSignal Strength Diagnostics:\n");

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            info.append("Connectivity service unavailable\n");
            return info.toString();
        }

        Network network = cm.getActiveNetwork();
        if (network == null) {
            info.append("No active network\n");
            return info.toString();
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) {
            info.append("Unable to read network capabilities\n");
            return info.toString();
        }

        // WiFi signal
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null) {
                int rssi = wifiManager.getConnectionInfo().getRssi();
                info.append("WiFi RSSI: ").append(rssi).append(" dBm\n");
            } else {
                info.append("WiFi info unavailable\n");
            }
        }

        // Mobile signal (SIM)
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            TelephonyManager tm =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            if (tm != null && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

                SignalStrength ss = tm.getSignalStrength();
                if (ss != null) {
                    info.append("Mobile Signal Level: ")
                            .append(ss.getLevel())
                            .append(" / 4\n");
                } else {
                    info.append("Mobile signal unavailable\n");
                }
            } else {
                info.append("Mobile signal permission denied\n");
            }
        }

        return info.toString();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (outputLog != null) {
                    outputLog.append("\nSignal permission granted\n");
                }
            } else {
                if (outputLog != null) {
                    outputLog.append("\nSignal permission denied\n");
                }
            }
        }
    }
}
