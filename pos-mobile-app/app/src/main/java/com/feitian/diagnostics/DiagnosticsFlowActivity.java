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
    private TextView leftToTestText;
    private View actionOverlay;
    private TextView overlayPromptText;
    private TextView overlayTimerText;
    private TextView touchHintText;
    private DrawingView flowDrawingView;
    private MaterialButton btnFinishInteractive;
    private TextView completionStatus;
    private MaterialButton btnDone;
    private MaterialButton btnPrintReceipt;
    
    private int completedCount = 0;
    private int totalTests = 0;
    private List<DiagnosticResult> latestResults;
    private CountDownTimer currentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics_flow);

        diagnosticsProgress = findViewById(R.id.diagnosticsProgress);
        testCountText = findViewById(R.id.testCountText);
        currentTestLabel = findViewById(R.id.currentTestLabel);
        leftToTestText = findViewById(R.id.leftToTestText);
        
        actionOverlay = findViewById(R.id.actionOverlay);
        overlayPromptText = findViewById(R.id.overlayPromptText);
        overlayTimerText = findViewById(R.id.overlayTimerText);
        touchHintText = findViewById(R.id.touchHintText);
        flowDrawingView = findViewById(R.id.flowDrawingView);
        btnFinishInteractive = findViewById(R.id.btnFinishInteractive);
        
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

        btnFinishInteractive.setOnClickListener(v -> {
            // Signal the Touch test to finish early
            TouchscreenTestV2.stopTest();
        });

        DiagnosticRunner runner = new DiagnosticRunner(getApplicationContext(), this);
        totalTests = runner.getTestCount();
        diagnosticsProgress.setMax(totalTests);
        
        // Initialize UI with accurate total
        TextView totalLabel = findViewById(R.id.totalTestCountLabel);
        if (totalLabel != null) {
            totalLabel.setText(getString(R.string.test_count_template, totalTests));
        }
        leftToTestText.setText(getString(R.string.left_to_test_template, totalTests));

        runner.runAll();
    }

    @Override
    public void onTestStarted(String testName) {
        runOnUiThread(() -> {
            currentTestLabel.setText(testName);
            cancelTimer();
            
            String prompt = "";
            int timeout = 0;
            boolean isTouchTest = "Touchscreen Test".equals(testName);

            switch (testName) {
                case "Touchscreen Test":
                    prompt = TouchscreenTestV2.getPrompt();
                    timeout = 30; 
                    break;
                case "NFC Tap Test":
                    prompt = NfcTestV2.getPrompt();
                    timeout = 10;
                    break;
                case "IC Card Test":
                    prompt = CardReaderTestV2.getIcPrompt();
                    timeout = 10;
                    break;
                case "Charging Port Test":
                    prompt = ChargingPortTestV2.getPrompt();
                    timeout = 10;
                    break;
            }

            if (timeout > 0) {
                overlayPromptText.setText(prompt);
                actionOverlay.setVisibility(View.VISIBLE);
                
                if (isTouchTest) {
                    flowDrawingView.setVisibility(View.VISIBLE);
                    flowDrawingView.clear();
                    touchHintText.setVisibility(View.VISIBLE);
                    btnFinishInteractive.setVisibility(View.GONE); 
                } else {
                    flowDrawingView.setVisibility(View.GONE);
                    touchHintText.setVisibility(View.GONE);
                    btnFinishInteractive.setVisibility(View.GONE);
                }
                
                startCountdown(timeout, isTouchTest);
            } else {
                actionOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void startCountdown(int seconds, boolean isTouchTest) {
        currentTimer = new CountDownTimer(seconds * 1000L, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                overlayTimerText.setText(String.format(Locale.US, "%.1fs", millisUntilFinished / 1000.0));
                
                if (isTouchTest && millisUntilFinished < (seconds - 3) * 1000L) {
                    btnFinishInteractive.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFinish() {
                overlayTimerText.setText(R.string.countdown_zero);
                if (isTouchTest) {
                    TouchscreenTestV2.stopTest(); // Auto-finish on timeout
                }
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
            actionOverlay.setVisibility(View.GONE);
            flowDrawingView.setVisibility(View.GONE);
            
            completedCount++;
            testCountText.setText(String.valueOf(completedCount));
            diagnosticsProgress.setProgress(completedCount);
            
            int left = totalTests - completedCount;
            leftToTestText.setText(getString(R.string.left_to_test_template, left));
        });
    }

    @Override
    public void onAllTestsComplete(List<DiagnosticResult> results) {
        this.latestResults = results;
        runOnUiThread(() -> {
            cancelTimer();
            actionOverlay.setVisibility(View.GONE);
            currentTestLabel.setText(R.string.all_tests_complete);
            leftToTestText.setText(R.string.status_test_complete);
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
