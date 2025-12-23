package com.feitian.diagnostics.diagnostics;

import android.content.Context;

public class TouchscreenTestV2 implements DiagnosticTest {

    private static volatile boolean touchDetected = false;

    public static void reset() {
        touchDetected = false;
    }

    public static void registerTouch() {
        touchDetected = true;
    }

    @Override
    public String getName() {
        return "Touchscreen Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        return getResult();
    }

    public static DiagnosticResult getResult() {
        String testName = "Touchscreen Test";

        if (touchDetected) {
            return new DiagnosticResult(
                    testName,
                    DiagnosticStatus.PASS,
                    "Touch detected successfully."
            );
        } else {
            return new DiagnosticResult(
                    testName,
                    DiagnosticStatus.FAIL,
                    "No touch detected during test window."
            );
        }
    }

    public static String getPrompt() {
        return "\nTouchscreen Test:\nPlease touch the screen now...\n";
    }
}
