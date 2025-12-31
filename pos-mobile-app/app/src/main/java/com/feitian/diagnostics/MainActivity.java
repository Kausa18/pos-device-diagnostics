package com.feitian.diagnostics;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feitian.diagnostics.diagnostics.BatteryTestV2;
import com.feitian.diagnostics.diagnostics.DeviceInfoTestV2;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticRunner;
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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DiagnosticRunner.DiagnosticListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView statusLabel;
    private TextView outputLog;
    private DiagnosticRunner runner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkAndRequestPermissions();

        statusLabel = findViewById(R.id.statusLabel);
        outputLog = findViewById(R.id.outputLog);
        MaterialButton btnRunAll = findViewById(R.id.btnRunAll);

        runner = new DiagnosticRunner(getApplicationContext(), this);

        btnRunAll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DiagnosticsFlowActivity.class);
            startActivity(intent);
        });
        
        setupGridItems();

        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                TouchscreenTestV2.registerTouch();
                v.performClick();
            }
            return false; 
        });
    }

    private void setupGridItems() {
        configureItem(R.id.btnDeviceInfo, "Device Info", R.drawable.ic_device, new DeviceInfoTestV2());
        configureItem(R.id.btnBattery, "Battery", R.drawable.ic_battery, new BatteryTestV2());
        configureItem(R.id.btnNetwork, "Network", R.drawable.ic_network, new NetworkTestV2());
        configureItem(R.id.btnSignal, "Signal", R.drawable.ic_signal, new SignalStrengthTestV2());
        configureItem(R.id.btnStorage, "Storage", R.drawable.ic_storage, new StorageTestV2());
        configureItem(R.id.btnPrinter, "Printer", R.drawable.ic_printer, new PrinterTestV2());
        configureItem(R.id.btnLedBuzzer, "LED/Buzzer", R.drawable.ic_peripherals, new LedBuzzerTestV2());
        configureItem(R.id.btnIcCard, "IC Card", R.drawable.ic_credit_card, new CardReaderTestV2.IcReaderTest());
        configureItem(R.id.btnNfcTap, "NFC Tap", R.drawable.ic_nfc, new NfcTestV2());
        configureItem(R.id.btnTouchscreen, "Touch", R.drawable.ic_touch, null);
        configureItem(R.id.btnCharging, "Charging", R.drawable.ic_charging, new ChargingPortTestV2());
        configureItem(R.id.btnViewHistory, "History", R.drawable.ic_history, null); // Named correctly as History
        
        findViewById(R.id.btnTouchscreen).setOnClickListener(v -> runTouchTest());
        findViewById(R.id.btnViewHistory).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void configureItem(int id, String label, int iconResId, DiagnosticTest test) {
        View item = findViewById(id);
        ((TextView) item.findViewById(R.id.iconLabel)).setText(label);
        ((ImageView) item.findViewById(R.id.iconImage)).setImageResource(iconResId);
        
        if (test != null) {
            item.setOnClickListener(v -> {
                statusLabel.setText(getString(R.string.running_test_prefix, label));
                outputLog.setText("");
                runner.runSingle(test);
            });
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
        boolean allGranted = true;
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void runTouchTest() {
        statusLabel.setText(getString(R.string.running_test_prefix, "Touchscreen Test"));
        outputLog.setText(TouchscreenTestV2.getPrompt());
        TouchscreenTestV2.reset();
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            DiagnosticResult res = TouchscreenTestV2.getResult();
            runOnUiThread(() -> onTestFinished(res));
        }).start();
    }

    @Override
    public void onTestStarted(String testName) {
        // MainActivity uses simple logging for individual tests
        outputLog.append("> Starting: " + testName + "\n");
    }

    @Override
    public void onTestFinished(DiagnosticResult result) {
        outputLog.setText(result.toString());
        Intent intent = new Intent(this, TestDetailActivity.class);
        intent.putExtra("test_name", result.getName());
        intent.putExtra("test_status", result.getStatus().name());
        intent.putExtra("test_details", result.getDetails());
        startActivity(intent);
    }

    @Override
    public void onAllTestsComplete(List<DiagnosticResult> results) {
        runOnUiThread(() -> statusLabel.setText(getString(R.string.status_test_complete)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
