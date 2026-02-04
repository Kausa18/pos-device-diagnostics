package com.feitian.diagnostics;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feitian.diagnostics.diagnostics.BatteryTestV2;
import com.feitian.diagnostics.diagnostics.DeviceInfoTestV2;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticRunner;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;
import com.feitian.diagnostics.diagnostics.NetworkTestV2;
import com.feitian.diagnostics.diagnostics.SignalStrengthTestV2;
import com.feitian.diagnostics.diagnostics.StorageTestV2;
import com.feitian.diagnostics.diagnostics.TouchscreenTestV2;
import com.feitian.diagnostics.diagnostics.hardware.CardReaderTestV2;
import com.feitian.diagnostics.diagnostics.hardware.ChargingPortTestV2;
import com.feitian.diagnostics.diagnostics.hardware.LedBuzzerTestV2;
import com.feitian.diagnostics.diagnostics.hardware.NfcTestV2;
import com.feitian.diagnostics.diagnostics.hardware.PrinterTestV2;
import com.ftpos.library.smartpos.servicemanager.OnServiceConnectCallback;
import com.ftpos.library.smartpos.servicemanager.ServiceManager;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DiagnosticRunner.DiagnosticListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String HISTORY_FILENAME = "diagnostics_history.json";
    
    private TextView totalScansText, lastStatusText, lastScanDateText, issuesCountText;
    private TextView statusLabel;
    private TextView outputLog;
    private DiagnosticRunner runner;
    private final Map<String, View> testIconViews = new HashMap<>();

    private View actionOverlay;
    private TextView overlayPromptText;
    private TextView overlayTimerText;
    private CountDownTimer currentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        bindHardwareService();

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkAndRequestPermissions();

        // Summary Header
        totalScansText = findViewById(R.id.totalScansText);
        lastStatusText = findViewById(R.id.lastStatusText);
        lastScanDateText = findViewById(R.id.lastScanDateText);
        issuesCountText = findViewById(R.id.issuesCountText);
        
        statusLabel = findViewById(R.id.statusLabel);
        outputLog = findViewById(R.id.outputLog);

        actionOverlay = findViewById(R.id.actionOverlay);
        overlayPromptText = findViewById(R.id.overlayPromptText);
        overlayTimerText = findViewById(R.id.overlayTimerText);
        
        View btnViewLastReport = findViewById(R.id.btnViewLastReport);
        if (btnViewLastReport != null) {
            btnViewLastReport.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        }

        View btnCloseResult = findViewById(R.id.btnCloseResult);
        if (btnCloseResult != null) {
            btnCloseResult.setOnClickListener(v -> {
                View overlay = findViewById(R.id.resultOverlay);
                if (overlay != null) overlay.setVisibility(View.GONE);
            });
        }
        
        MaterialButton btnRunAll = findViewById(R.id.btnRunAll);
        if (btnRunAll != null) {
            btnRunAll.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DiagnosticsFlowActivity.class);
                startActivity(intent);
            });
        }

        runner = new DiagnosticRunner(getApplicationContext(), this);

        setupGridItems();
        loadDashboardStats();

        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                TouchscreenTestV2.registerTouch();
                v.performClick();
            }
            return false; 
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        int totalScans = 0;
        int lastStatusRes = R.string.dashboard_state_ready;
        int issuesCount = 0;
        String lastDate = getString(R.string.dashboard_never_scanned);
        int lastColor = ContextCompat.getColor(this, R.color.status_pass_text);

        try (FileInputStream fis = openFileInput(HISTORY_FILENAME);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            String lastLine = null;
            while ((line = reader.readLine()) != null) {
                totalScans++;
                lastLine = line;
            }

            if (lastLine != null) {
                JSONObject record = new JSONObject(lastLine);
                lastDate = record.optString("timestamp", getString(R.string.dashboard_never_scanned));
                
                JSONArray results = record.optJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject testObj = results.getJSONObject(i);
                        if (!testObj.optString("status").equalsIgnoreCase("PASS")) {
                            issuesCount++;
                        }
                    }
                    lastStatusRes = (issuesCount > 0) ? R.string.dashboard_state_issues : R.string.dashboard_state_healthy;
                    lastColor = (issuesCount > 0) ? ContextCompat.getColor(this, R.color.status_fail_text) 
                                                 : ContextCompat.getColor(this, R.color.status_pass_text);
                }
            }
        } catch (Exception ignored) {}

        if (totalScansText != null) totalScansText.setText(String.valueOf(totalScans));
        if (lastStatusText != null) {
            lastStatusText.setText(lastStatusRes);
            lastStatusText.setTextColor(lastColor);
        }
        if (lastScanDateText != null) lastScanDateText.setText(getString(R.string.last_scan_prefix, lastDate));
        
        View btnViewLastReport = findViewById(R.id.btnViewLastReport);
        if (issuesCountText != null) {
            if (totalScans > 0) {
                if (btnViewLastReport != null) btnViewLastReport.setVisibility(View.VISIBLE);
                if (issuesCount > 0) {
                    issuesCountText.setText(getString(R.string.dashboard_issues_detected, issuesCount));
                    issuesCountText.setTextColor(ContextCompat.getColor(this, R.color.status_fail_text));
                } else {
                    issuesCountText.setText(R.string.all_systems_healthy);
                    issuesCountText.setTextColor(ContextCompat.getColor(this, R.color.status_pass_text));
                }
            } else {
                if (btnViewLastReport != null) btnViewLastReport.setVisibility(View.GONE);
                issuesCountText.setText(R.string.all_systems_healthy);
            }
        }
    }

    private void bindHardwareService() {
        ServiceManager.bindPosServer(getApplicationContext(), new OnServiceConnectCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusLabel != null) statusLabel.setText(R.string.status_hardware_ready);
                });
            }
            @Override
            public void onFail(int error) {
                runOnUiThread(() -> {
                    if (statusLabel != null) statusLabel.setText(getString(R.string.status_hardware_failed, error));
                });
            }
        });
    }

    private void setupGridItems() {
        registerTestItem(R.id.btnDeviceInfo, R.drawable.ic_device, new DeviceInfoTestV2());
        registerTestItem(R.id.btnBattery, R.drawable.ic_battery, new BatteryTestV2());
        registerTestItem(R.id.btnNetwork, R.drawable.ic_network, new NetworkTestV2());
        registerTestItem(R.id.btnSignal, R.drawable.ic_signal, new SignalStrengthTestV2());
        registerTestItem(R.id.btnStorage, R.drawable.ic_storage, new StorageTestV2());
        registerTestItem(R.id.btnPrinter, R.drawable.ic_printer, new PrinterTestV2());
        registerTestItem(R.id.btnLedBuzzer, R.drawable.ic_peripherals, new LedBuzzerTestV2());
        
        registerTestItem(R.id.btnIcCard, R.drawable.ic_credit_card, new CardReaderTestV2.IcReaderTest());
        registerTestItem(R.id.btnNfcTap, R.drawable.ic_nfc, new NfcTestV2());
        registerTestItem(R.id.btnTouchscreen, R.drawable.ic_touch, new TouchscreenTestV2());
        registerTestItem(R.id.btnCharging, R.drawable.ic_charging, new ChargingPortTestV2());
        
        View historyView = findViewById(R.id.btnViewHistory);
        if (historyView != null) {
            TextView label = historyView.findViewById(R.id.iconLabel);
            ImageView icon = historyView.findViewById(R.id.iconImage);
            if (label != null) label.setText(R.string.label_history);
            if (icon != null) icon.setImageResource(R.drawable.ic_history);
            historyView.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        }
    }

    private void registerTestItem(int id, int iconResId, DiagnosticTest test) {
        View item = findViewById(id);
        if (item == null) return;

        TextView labelView = item.findViewById(R.id.iconLabel);
        ImageView iconView = item.findViewById(R.id.iconImage);
        
        if (labelView != null) labelView.setText(test.getName());
        if (iconView != null) iconView.setImageResource(iconResId);
        
        testIconViews.put(test.getName(), item);
        
        item.setOnClickListener(v -> {
            if (test instanceof TouchscreenTestV2) {
                TouchscreenTestV2.reset();
                startActivity(new Intent(this, TouchTestActivity.class));
                new Thread(() -> {
                    DiagnosticResult res = test.run(this);
                    runOnUiThread(() -> onTestFinished(res));
                }).start();
            } else {
                if (statusLabel != null) statusLabel.setText(getString(R.string.status_running_diagnostics));
                runner.runSingle(test);
            }
        });
    }

    private void showResultOverlay(DiagnosticResult result) {
        runOnUiThread(() -> {
            View overlay = findViewById(R.id.resultOverlay);
            if (overlay == null) return;
            overlay.setVisibility(View.VISIBLE);

            TextView title = findViewById(R.id.resultTitle);
            TextView details = findViewById(R.id.resultDetails);
            TextView statusBadge = findViewById(R.id.resultStatusBadge);
            ImageView icon = findViewById(R.id.resultIcon);
            View btnRetry = findViewById(R.id.btnRetryResult);

            if (title != null) title.setText(result.getName());
            if (details != null) details.setText(result.getDetails());
            
            boolean isPass = result.getStatus() == DiagnosticStatus.PASS;
            if (icon != null) {
                icon.setImageResource(isPass ? R.drawable.ic_check_circle : android.R.drawable.ic_delete);
                icon.setColorFilter(ContextCompat.getColor(this, isPass ? R.color.status_pass : R.color.status_fail));
            }
            
            if (statusBadge != null) {
                statusBadge.setText(isPass ? R.string.status_passed_caps : R.string.status_failed_caps);
                statusBadge.setBackgroundResource(isPass ? R.drawable.badge_bg_pass : R.drawable.badge_bg_fail);
            }
            
            if (btnRetry != null) {
                btnRetry.setOnClickListener(v -> {
                    overlay.setVisibility(View.GONE);
                    View testItem = testIconViews.get(result.getName());
                    if (testItem != null) {
                        testItem.performClick();
                    }
                });
            }
        });
    }

    private void checkAndRequestPermissions() {
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onTestStarted(String testName) {
        runOnUiThread(() -> {
            if (outputLog != null) outputLog.append(getString(R.string.log_starting_test, testName));

            cancelTimer();

            String prompt = "";
            int timeoutSeconds = 0;
            switch (testName) {
                case "NFC Tap Test":
                    prompt = NfcTestV2.getPrompt();
                    timeoutSeconds = 10;
                    break;
                case "IC Card Test":
                    prompt = CardReaderTestV2.getIcPrompt();
                    timeoutSeconds = 10;
                    break;
                case "Charging Port Test":
                    prompt = ChargingPortTestV2.getPrompt();
                    timeoutSeconds = 10;
                    break;
            }

            if (timeoutSeconds > 0 && actionOverlay != null && overlayPromptText != null && overlayTimerText != null) {
                overlayPromptText.setText(prompt);
                actionOverlay.setVisibility(View.VISIBLE);
                startCountdown(timeoutSeconds);
            } else if (actionOverlay != null) {
                actionOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void startCountdown(int seconds) {
        if (overlayTimerText == null) return;

        currentTimer = new CountDownTimer(seconds * 1000L, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                overlayTimerText.setText(String.format(java.util.Locale.US, "%.1fs", millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                overlayTimerText.setText(R.string.countdown_zero);
            }
        }.start();
    }

    private void cancelTimer() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }

    @Override
    public void onTestFinished(DiagnosticResult result) {
        runOnUiThread(() -> {
            cancelTimer();
            if (actionOverlay != null) actionOverlay.setVisibility(View.GONE);
            if (outputLog != null) outputLog.setText(result.toString());
            showResultOverlay(result);
        });
    }

    @Override
    public void onAllTestsComplete(List<DiagnosticResult> results) {
        runOnUiThread(() -> {
            if (statusLabel != null) statusLabel.setText(R.string.status_test_complete);
            loadDashboardStats();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
