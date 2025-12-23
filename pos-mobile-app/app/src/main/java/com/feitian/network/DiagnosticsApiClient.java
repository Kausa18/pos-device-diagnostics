package com.feitian.network;

import android.util.Log;
import com.feitian.diagnostics.BuildConfig;
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
    
    // Updated to your computer's LAN IP 
    private static final String ENDPOINT = "http://192.168.100.42:4000/diagnostics";

    public static String send(JSONObject payload) {
        HttpURLConnection connection = null;

        try {
            Log.d(TAG, "Sending payload: " + payload);
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Get API key from BuildConfig with fallback
            String apiKey = "supersecret123";
            try {
                apiKey = BuildConfig.API_KEY;
            } catch (Throwable ignored) {}
            
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);
            
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
                return "Server Error " + responseCode + (errorResponse.length() > 0 ? ": " + errorResponse : "");
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
