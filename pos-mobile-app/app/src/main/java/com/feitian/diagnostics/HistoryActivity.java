package com.feitian.diagnostics;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private static final String FILENAME = "diagnostics_history.json";
    private TextView summaryText;
    private TextView historyLog;
    private final List<String> allLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        summaryText = findViewById(R.id.summaryText);
        historyLog = findViewById(R.id.historyLog);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnExportCsv = findViewById(R.id.btnExportCsv);

        Button btnFilterAll = findViewById(R.id.btnFilterAll);
        Button btnFilterPass = findViewById(R.id.btnFilterPass);
        Button btnFilterFail = findViewById(R.id.btnFilterFail);

        loadHistoryAndCalculateStats();

        btnBack.setOnClickListener(v -> finish());
        btnExportCsv.setOnClickListener(v -> exportCsv());

        btnFilterAll.setOnClickListener(v -> applyFilter("ALL"));
        btnFilterPass.setOnClickListener(v -> applyFilter("PASS"));
        btnFilterFail.setOnClickListener(v -> applyFilter("FAIL"));
    }

    private void loadHistoryAndCalculateStats() {
        allLogs.clear();
        int totalTests = 0;
        int passCount = 0;
        int failCount = 0;

        try (FileInputStream fis = openFileInput(FILENAME);
             InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                allLogs.add(line);

                try {
                    JSONObject record = new JSONObject(line);
                    JSONArray results = record.optJSONArray("results");
                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject test = results.getJSONObject(i);
                            String status = test.optString("status");
                            totalTests++;
                            if ("PASS".equalsIgnoreCase(status) || "PASSED".equalsIgnoreCase(status)) {
                                passCount++;
                            } else {
                                failCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for stats
                }
            }
        } catch (Exception e) {
            // No history found
        }

        if (totalTests > 0) {
            int successRate = (int) (((float) passCount / totalTests) * 100);
            
            String summaryStr = String.format(Locale.US, "Total Tests: %d\nPASSED: %d | FAILED: %d\nSuccess Rate: %d%%",
                    totalTests, passCount, failCount, successRate);
            SpannableString summary = new SpannableString(summaryStr);

            int greenColor = Color.parseColor("#4CAF50");
            int redColor = Color.parseColor("#F44336");

            int passIndex = summaryStr.indexOf("PASSED");
            int pipeIndex = summaryStr.indexOf("|");
            int failIndex = summaryStr.indexOf("FAILED");
            int successIndex = summaryStr.indexOf("\nSuccess");

            if (passIndex != -1 && pipeIndex != -1) {
                summary.setSpan(new ForegroundColorSpan(greenColor), passIndex, pipeIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (failIndex != -1 && successIndex != -1) {
                summary.setSpan(new ForegroundColorSpan(redColor), failIndex, successIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            summaryText.setText(summary);
        } else {
            summaryText.setText(R.string.no_history_data);
        }

        applyFilter("ALL");
    }

    private void applyFilter(String filter) {
        StringBuilder displayLog = new StringBuilder();

        for (String jsonLine : allLogs) {
            try {
                JSONObject record = new JSONObject(jsonLine);
                JSONArray results = record.optJSONArray("results");
                String timestamp = record.optString("timestamp", "Unknown Date");
                
                boolean showRecord = false;
                StringBuilder recordDisplay = new StringBuilder();
                recordDisplay.append("Date: ").append(timestamp).append("\n");

                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject test = results.getJSONObject(i);
                        String name = test.optString("name");
                        String status = test.optString("status");

                        boolean isPass = "PASS".equalsIgnoreCase(status) || "PASSED".equalsIgnoreCase(status);
                        boolean matchesFilter = filter.equals("ALL") || 
                                              (filter.equals("PASS") && isPass) || 
                                              (filter.equals("FAIL") && !isPass);

                        if (matchesFilter) {
                            showRecord = true;
                            recordDisplay.append("  ").append(name).append(": ").append(status).append("\n");
                        }
                    }
                }
                
                if (showRecord) {
                    displayLog.append(recordDisplay).append("\n");
                }

            } catch (Exception e) {
                // Skip bad lines
            }
        }
        
        if (displayLog.length() == 0) {
            historyLog.setText(getString(R.string.no_history_records_found, filter));
        } else {
            historyLog.setText(displayLog.toString());
        }
    }

    private void exportCsv() {
        File exportDir = new File(getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Toast.makeText(this, "Failed to create export directory", Toast.LENGTH_SHORT).show();
            return;
        }
        File csvFile = new File(exportDir, "diagnostics_export.csv");
        
        try (FileOutputStream fos = new FileOutputStream(csvFile)) {
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("Timestamp,DeviceModel,TestName,Status,Details\n");

            try (FileInputStream fis = openFileInput(FILENAME);
                 InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JSONObject record = new JSONObject(line);
                        String timestamp = record.optString("timestamp", "N/A");
                        String model = record.optString("model", "Unknown");
                        
                        JSONArray results = record.optJSONArray("results");
                        if (results != null) {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject test = results.getJSONObject(i);
                                String name = test.optString("name");
                                String status = test.optString("status");
                                String details = test.optString("details").replace(",", ";").replace("\n", " ");
                                
                                csvBuilder.append(timestamp).append(",")
                                        .append(model).append(",")
                                        .append(name).append(",")
                                        .append(status).append(",")
                                        .append(details).append("\n");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing line for CSV", e);
                    }
                }
            }
            
            fos.write(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
            shareFile(csvFile);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to create CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "CSV export failed", e);
        }
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Diagnostics CSV"));
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "File provider error", e);
        }
    }
}
