package com.feitian.network;

import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiagnosticsApiClient {

    private static final String TAG = "DiagnosticsApiClient";
    
    // Using your computer's LAN IP
    private static final String ENDPOINT = "http://192.168.100.19 :4000/diagnostics";

    public static String send(JSONObject payload) {
        HttpURLConnection connection = null;

        try {
            Log.d(TAG, "Connecting to: " + ENDPOINT);
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Hardcoding the key to 100% resolve "Unauthorized"
            connection.setRequestProperty("x-api-key", "supersecret123");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP Response: " + responseCode);
            
            if (responseCode == 200 || responseCode == 201) {
                return null; // Success
            } else {
                StringBuilder errorResponse = new StringBuilder();
                InputStream es = connection.getErrorStream();
                if (es != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorResponse.append(line.trim());
                        }
                    }
                }
                return "Error " + responseCode + ": " + (errorResponse.length() > 0 ? errorResponse : "No message");
            }

        } catch (Exception e) {
            Log.e(TAG, "Connection Failed: " + e.getMessage());
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
