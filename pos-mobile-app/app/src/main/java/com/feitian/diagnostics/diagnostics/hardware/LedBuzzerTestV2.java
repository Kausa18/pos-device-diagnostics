package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;

public class LedBuzzerTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "LED/Buzzer Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        boolean buzzerTriggered = false;
        boolean ledTriggered = false;

        try {
            // 1. Try buzzer
            try {
                com.ftpos.library.smartpos.buzzer.Buzzer buzzer = com.ftpos.library.smartpos.buzzer.Buzzer.getInstance(context);
                if (buzzer != null) {
                    buzzer.beep(1, 200, 200, 0);
                    buzzerTriggered = true;
                }
            } catch (Throwable ignored) {}

            // 2. Try LED
            try {
                com.ftpos.library.smartpos.led.Led led = com.ftpos.library.smartpos.led.Led.getInstance(context);
                if (led != null) {
                    led.ledStatus(true, true, true, true);
                    ledTriggered = true;

                    // IMMEDIATELY turn them OFF after a short delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            com.ftpos.library.smartpos.led.Led.getInstance(context).ledStatus(false, false, false, false);
                        } catch (Throwable ignored) {}
                    }, 800); // Reduced to 800ms
                }
            } catch (Throwable ignored) {}

            if (buzzerTriggered || ledTriggered) {
                return new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Triggered Successfully");
            } else {
                return new DiagnosticResult(getName(), DiagnosticStatus.WARNING, "Not Triggered");
            }

        } catch (Exception e) {
            return new DiagnosticResult(getName(), DiagnosticStatus.SKIPPED, "Error: " + e.getMessage());
        }
    }
}
