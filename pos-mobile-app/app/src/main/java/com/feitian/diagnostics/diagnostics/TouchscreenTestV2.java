package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TouchscreenTestV2 implements DiagnosticTest {

    private static volatile boolean touchDetected = false;
    private static CountDownLatch latch;

    public static void reset() {
        touchDetected = false;
        latch = new CountDownLatch(1);
    }

    /**
     * Marks that the user has interacted with the screen.
     * Does NOT conclude the test to allow for continuous drawing.
     */
    public static void registerTouch() {
        touchDetected = true;
    }

    /**
     * Specifically used to conclude the blocking run() method.
     */
    public static void stopTest() {
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public String getName() {
        return "Touchscreen Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        reset();
        boolean finishedInTime;
        try {
            // Await returns true if the count reached zero and false if the waiting time elapsed
            finishedInTime = latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "Test interrupted.");
        }

        if (touchDetected) {
            return new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Touch area verified successfully.");
        } else {
            String failureReason = finishedInTime ? "Test finished without touch registration." : "No interaction detected within 30 seconds.";
            return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, failureReason);
        }
    }

    public static DiagnosticResult getResult() {
        if (touchDetected) {
            return new DiagnosticResult("Touchscreen Test", DiagnosticStatus.PASS, "Touch verified.");
        } else {
            return new DiagnosticResult("Touchscreen Test", DiagnosticStatus.FAIL, "No touch detected.");
        }
    }

    public static String getPrompt() {
        return "\nTouchscreen Test:\nPlease draw across the entire screen...\n";
    }
}
