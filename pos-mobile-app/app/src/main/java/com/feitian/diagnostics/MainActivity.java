package com.feitian.diagnostics;

import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticRunner;
import com.feitian.diagnostics.diagnostics.DiagnosticsPayloadBuilder;
import com.feitian.diagnostics.diagnostics.TouchscreenTestV2;
import com.feitian.network.DiagnosticsApiClient;

import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DiagnosticRunner.DiagnosticListener {

    private TextView statusLabel;
    private TextView outputLog;
    private Button btnRunDiagnostics;

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        findViewById(R.id.main).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!btnRunDiagnostics.isEnabled()) {
                    TouchscreenTestV2.registerTouch();
                }
                v.performClick();
            }
            return false; 
        });

        btnRunDiagnostics = findViewById(R.id.btnRunDiagnostics);
        statusLabel = findViewById(R.id.statusLabel);
        outputLog = findViewById(R.id.outputLog);

        btnRunDiagnostics.setOnClickListener(v -> startDiagnostics());
    }

    private void startDiagnostics() {
        btnRunDiagnostics.setEnabled(false);
        statusLabel.setText(R.string.status_running_diagnostics);
        outputLog.setText(R.string.starting_diagnostics);
        outputLog.append("\n");

        DiagnosticRunner runner = new DiagnosticRunner(getApplicationContext(), this);
        runner.runAll();
    }

    @Override
    public void onTestStarted(String testName) {
        if (testName.equals("Touchscreen Test")) {
            outputLog.append(TouchscreenTestV2.getPrompt());
        }
    }

    @Override
    public void onTestFinished(DiagnosticResult result) {
        outputLog.append(result.toString());
        outputLog.append("\n");
    }

    @Override
    public void onAllTestsComplete(List<DiagnosticResult> results) {
        runOnUiThread(() -> statusLabel.setText("Status: Uploading results..."));

        // Updated: Pass context to build() for persistent Device ID
        JSONObject payload = DiagnosticsPayloadBuilder.build(getApplicationContext(), results);
        outputLog.append("\n--- JSON Payload Generated ---\n");
        outputLog.append(payload.toString());
        outputLog.append("\n------------------------------\n");

        new Thread(() -> {
            String errorMessage = DiagnosticsApiClient.send(payload);
            
            runOnUiThread(() -> {
                if (errorMessage == null) {
                    statusLabel.setText("Status: Upload successful");
                    outputLog.append("\nUpload: SUCCESS\n");
                } else {
                    statusLabel.setText("Status: Upload failed");
                    outputLog.append("\nUpload: FAILED\nReason: " + errorMessage + "\n");
                }
                btnRunDiagnostics.setEnabled(true);
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (outputLog != null) outputLog.append("\nSignal permission granted\n");
        }
    }
}
