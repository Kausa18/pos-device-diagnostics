package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.feitian.diagnostics.diagnostics.hardware.CardReaderTestV2;
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
    private final List<DiagnosticTest> tests = new ArrayList<>();
    private final List<DiagnosticResult> results = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DiagnosticRunner(Context context, DiagnosticListener listener) {
        this.context = context;
        this.listener = listener;

        // Register Automated Tests
        tests.add(new DeviceInfoTestV2());
        tests.add(new BatteryTestV2());
        tests.add(new NetworkTestV2());
        tests.add(new SignalStrengthTestV2());
        tests.add(new PrinterTestV2());
        tests.add(new NfcTestV2());
        tests.add(new CardReaderTestV2());
    }

    public void runAll() {
        results.clear();
        new Thread(() -> {
            // 1. Run automated tests
            for (DiagnosticTest test : tests) {
                runSingleTest(test);
            }

            // 2. Run Interactive Touchscreen Test
            mainHandler.post(() -> listener.onTestStarted("Touchscreen Test"));
            TouchscreenTestV2.reset();
            
            // Wait for user interaction (handled in MainActivity, we just wait here)
            try {
                Thread.sleep(5000); 
            } catch (InterruptedException ignored) {}

            DiagnosticResult touchResult = TouchscreenTestV2.getResult();
            results.add(touchResult);
            mainHandler.post(() -> listener.onTestFinished(touchResult));

            // 3. Finalize
            mainHandler.post(() -> listener.onAllTestsComplete(new ArrayList<>(results)));
        }).start();
    }

    private void runSingleTest(DiagnosticTest test) {
        final String testName = test.getName();
        mainHandler.post(() -> listener.onTestStarted(testName));

        try {
            Thread.sleep(500); // UI spacing
        } catch (InterruptedException ignored) {}

        final DiagnosticResult result = test.run(context);
        results.add(result);
        mainHandler.post(() -> listener.onTestFinished(result));
    }
}
