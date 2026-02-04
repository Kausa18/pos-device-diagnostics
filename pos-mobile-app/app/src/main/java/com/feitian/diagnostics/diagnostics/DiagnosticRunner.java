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
import java.util.Arrays;
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

    public List<DiagnosticTest> getAllTests() {
        return Arrays.asList(
                new DeviceInfoTestV2(),
                new BatteryTestV2(),
                new NetworkTestV2(),
                new SignalStrengthTestV2(),
                new StorageTestV2(),
                new PrinterTestV2(),
                new LedBuzzerTestV2(),
                new ChargingPortTestV2(),
                new CardReaderTestV2.IcReaderTest(),
                new NfcTestV2(),
                new TouchscreenTestV2()
        );
    }

    public int getTestCount() {
        return getAllTests().size();
    }

    public void runAll() {
        results.clear();
        new Thread(() -> {
            List<DiagnosticTest> allTests = getAllTests();
            for (DiagnosticTest test : allTests) {
                runSingleSync(test);
            }
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
