package com.feitian.diagnostics;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticRunner;
import com.feitian.diagnostics.diagnostics.DiagnosticsPayloadBuilder;
import com.feitian.diagnostics.diagnostics.TouchscreenTestV2;
import com.feitian.diagnostics.diagnostics.hardware.CardReaderTestV2;
import com.feitian.diagnostics.diagnostics.hardware.NfcTestV2;
import com.feitian.diagnostics.diagnostics.hardware.ChargingPortTestV2;
import com.feitian.diagnostics.diagnostics.hardware.PrinterHelper;
import com.feitian.network.DiagnosticsApiClient;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

public class DiagnosticsFlowActivity extends AppCompatActivity implements DiagnosticRunner.DiagnosticListener {

    private static final String HISTORY_FILENAME = "diagnostics_history.json";
    
    private ProgressBar diagnosticsProgress;
    private TextView testCountText;
    private TextView currentTestLabel;
    private TextView promptText;
    private TextView countdownTimer;
    private TextView completionStatus;
    private MaterialButton btnDone;
    private MaterialButton btnPrintReceipt;
    
    private int completedCount = 0;
    private List<DiagnosticResult> latestResults;
    private CountDownTimer currentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics_flow);

        diagnosticsProgress = findViewById(R.id.diagnosticsProgress);
        testCountText = findViewById(R.id.testCountText);
        currentTestLabel = findViewById(R.id.currentTestLabel);
        promptText = findViewById(R.id.promptText);
        countdownTimer = findViewById(R.id.countdownTimer);
        completionStatus = findViewById(R.id.completionStatus);
        btnDone = findViewById(R.id.btnDone);
        btnPrintReceipt = findViewById(R.id.btnPrintReceipt);

        btnDone.setOnClickListener(v -> finish());
        
        btnPrintReceipt.setOnClickListener(v -> {
            if (latestResults != null) {
                JSONObject payload = DiagnosticsPayloadBuilder.build(getApplicationContext(), latestResults);
                String terminalId = payload.optString("terminal_id", "Unknown");
                PrinterHelper.printReceipt(getApplicationContext(), latestResults, terminalId);
            }
        });

        DiagnosticRunner runner = new DiagnosticRunner(getApplicationContext(), this);
        runner.runAll();
    }

    @Override
    public void onTestStarted(String testName) {
        runOnUiThread(() -> {
            currentTestLabel.setText(testName);
            cancelTimer(); // Clear any previous timer
            
            // Show prompts and timers for interactive tests
            switch (testName) {
                case "Touchscreen Test":
                    promptText.setText(TouchscreenTestV2.getPrompt());
                    startCountdown(5); // 5s for touch
                    break;
                case "NFC Tap Test":
                    promptText.setText(NfcTestV2.getPrompt());
                    startCountdown(10); // 10s for nfc
                    break;
                case "IC Card Test":
                    promptText.setText(CardReaderTestV2.getIcPrompt());
                    startCountdown(10); // 10s for ic
                    break;
                case "Charging Port Test":
                    promptText.setText(ChargingPortTestV2.getPrompt());
                    startCountdown(10); // 10s for charging
                    break;
                default:
                    promptText.setText("");
                    countdownTimer.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void startCountdown(int seconds) {
        countdownTimer.setVisibility(View.VISIBLE);
        currentTimer = new CountDownTimer(seconds * 1000L, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Use Locale.US for decimal points consistency
                countdownTimer.setText(String.format(Locale.US, "%.1fs", millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                countdownTimer.setText(R.string.countdown_zero);
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
            cancelTimer(); // Stop timer as soon as action is detected
            countdownTimer.setVisibility(View.GONE);
            completedCount++;
            testCountText.setText(String.valueOf(completedCount));
            diagnosticsProgress.setProgress((completedCount * 100) / 11);
        });
    }

    @Override
    public void onAllTestsComplete(List<DiagnosticResult> results) {
        this.latestResults = results;
        runOnUiThread(() -> {
            cancelTimer();
            countdownTimer.setVisibility(View.GONE);
            currentTestLabel.setText(R.string.all_tests_complete);
            promptText.setText("");
            completionStatus.setVisibility(View.VISIBLE);
            completionStatus.setText(R.string.finalizing_report);
            btnDone.setVisibility(View.VISIBLE);
            btnPrintReceipt.setVisibility(View.VISIBLE);
        });

        JSONObject payload = DiagnosticsPayloadBuilder.build(getApplicationContext(), results);
        saveToHistory(payload);

        new Thread(() -> {
            String errorMessage = DiagnosticsApiClient.send(payload);
            runOnUiThread(() -> {
                if (errorMessage == null) {
                    completionStatus.setText(R.string.upload_successful);
                } else {
                    completionStatus.setText(getString(R.string.upload_failed_with_reason, errorMessage));
                    completionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_fail));
                }
            });
        }).start();
    }

    private void saveToHistory(JSONObject payload) {
        try (FileOutputStream fos = openFileOutput(HISTORY_FILENAME, MODE_APPEND)) {
            fos.write((payload.toString() + "\n").getBytes());
        } catch (Exception e) {
            Log.e("FlowActivity", "History error", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
