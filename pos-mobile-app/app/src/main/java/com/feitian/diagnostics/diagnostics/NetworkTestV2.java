package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Network Connectivity";
    }

    @Override
    public DiagnosticResult run(Context context) {
        DiagnosticStatus status;
        StringBuilder details = new StringBuilder();

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                return new DiagnosticResult(
                        getName(),
                        DiagnosticStatus.FAIL,
                        "ConnectivityManager unavailable"
                );
            }

            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) {
                return new DiagnosticResult(
                        getName(),
                        DiagnosticStatus.FAIL,
                        "No active network connection"
                );
            }

            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            if (caps == null) {
                return new DiagnosticResult(
                        getName(),
                        DiagnosticStatus.FAIL,
                        "Unable to read network capabilities"
                );
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

            details.append("Connection Type: ").append(connectionType).append("\n");
            details.append("Internet Available: ").append(hasInternet ? "Yes" : "No");

            status = hasInternet ? DiagnosticStatus.PASS : DiagnosticStatus.FAIL;

        } catch (Exception e) {
            status = DiagnosticStatus.FAIL;
            details.append("Error checking network: ").append(e.getMessage());
        }

        return new DiagnosticResult(getName(), status, details.toString());
    }
}
