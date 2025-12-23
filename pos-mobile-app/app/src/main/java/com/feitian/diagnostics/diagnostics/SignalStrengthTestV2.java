package com.feitian.diagnostics.diagnostics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class SignalStrengthTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Signal Strength";
    }

    @Override
    public DiagnosticResult run(Context context) {
        DiagnosticStatus status;
        StringBuilder details = new StringBuilder();

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "Connectivity unavailable");
            }

            Network network = cm.getActiveNetwork();
            if (network == null) {
                return new DiagnosticResult(getName(), DiagnosticStatus.SKIPPED, "No active network");
            }

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) {
                return new DiagnosticResult(getName(), DiagnosticStatus.SKIPPED, "Network capabilities unavailable");
            }

            boolean foundSignal = false;

            // WiFi signal
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifi =
                        (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                if (wifi != null) {
                    int rssi = wifi.getConnectionInfo().getRssi();
                    details.append("WiFi RSSI: ").append(rssi).append(" dBm\n");
                    foundSignal = true;
                }
            }

            // SIM signal
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                    TelephonyManager tm =
                            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                    if (tm != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            SignalStrength ss = tm.getSignalStrength();
                            if (ss != null) {
                                details.append("SIM Signal Level: ")
                                        .append(ss.getLevel())
                                        .append(" / 4\n");
                                foundSignal = true;
                            }
                        } else {
                            details.append("SIM signal level detection requires API 28+\n");
                        }
                    }
                } else {
                    details.append("SIM signal permission not granted\n");
                }
            }

            status = foundSignal ? DiagnosticStatus.PASS : DiagnosticStatus.SKIPPED;

        } catch (Exception e) {
            status = DiagnosticStatus.FAIL;
            details.append("Error: ").append(e.getMessage());
        }

        return new DiagnosticResult(getName(), status, details.toString());
    }
}
