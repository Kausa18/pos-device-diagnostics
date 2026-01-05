package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.feitian.diagnostics.diagnostics.hardware.CardReaderTestV2;
import com.feitian.diagnostics.diagnostics.hardware.ChargingPortTestV2;
import com.feitian.diagnostics.diagnostics.hardware.LedBuzzerTestV2;
import com.feitian.diagnostics.diagnostics.hardware.NfcTestV2;
import com.feitian.diagnostics.diagnostics.hardware.PrinterTestV2;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticRunner {
    public interface DiagnosticListener {
        void onTestStarted(String testName);
        void onTestFinished(DiagnosticResult result);
        void onAllTestsComplete(List<DiagnosticResult> results);
    }

    private final Context context;
    private final DiagnosticListener listener;
    private final List<DiagnosticResult> results = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DiagnosticRunner(Context context, DiagnosticListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void runAll() {
        results.clear();
        new Thread(() -> {
            // 1. Automated Tests
            runSingleSync(new DeviceInfoTestV2());
            runSingleSync(new BatteryTestV2());
            runSingleSync(new NetworkTestV2());
            runSingleSync(new SignalStrengthTestV2());
            runSingleSync(new StorageTestV2());
            runSingleSync(new PrinterTestV2());
            runSingleSync(new LedBuzzerTestV2());

            // 2. Interactive Tests
            runSingleSync(new ChargingPortTestV2());
            runSingleSync(new CardReaderTestV2.IcReaderTest());
            runSingleSync(new NfcTestV2());
            
            // 3. Touchscreen Test (Now integrated properly)
            runSingleSync(new TouchscreenTestV2());

            mainHandler.post(() -> listener.onAllTestsComplete(new ArrayList<>(results)));
        }).start();
    }

    public void runSingle(DiagnosticTest test) {
        results.clear();
        new Thread(() -> {
            runSingleSync(test);
            mainHandler.post(() -> listener.onAllTestsComplete(new ArrayList<>(results)));
        }).start();
    }

    private void runSingleSync(DiagnosticTest test) {
        final String testName = test.getName();
        mainHandler.post(() -> listener.onTestStarted(testName));
        
        // Slight delay for UI state transition
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        
        final DiagnosticResult result = test.run(context);
        results.add(result);
        mainHandler.post(() -> listener.onTestFinished(result));
    }
}
