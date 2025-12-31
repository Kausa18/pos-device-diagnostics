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
                    // Beep once: times=1, on=200ms, off=200ms, mode=0
                    buzzer.beep(1, 200, 200, 0);
                    buzzerTriggered = true;
                }
            } catch (Throwable ignored) {}

            // 2. Try LED
            try {
                com.ftpos.library.smartpos.led.Led led = com.ftpos.library.smartpos.led.Led.getInstance(context);
                if (led != null) {
                    // Turn all 4 LEDs ON (Red, Yellow, Green, Blue)
                    led.ledStatus(true, true, true, true);
                    ledTriggered = true;

                    // Automatically turn them OFF after 1.5 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            led.ledStatus(false, false, false, false);
                        } catch (Throwable ignored) {}
                    }, 1500);
                }
            } catch (Throwable ignored) {}

            if (buzzerTriggered || ledTriggered) {
                String components = (buzzerTriggered ? "Buzzer " : "") + (ledTriggered ? "LED" : "");
                return new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Triggered: " + components.trim());
            } else {
                return new DiagnosticResult(getName(), DiagnosticStatus.WARNING, "LED/Buzzer initialized but did not trigger");
            }

        } catch (Exception e) {
            return new DiagnosticResult(getName(), DiagnosticStatus.SKIPPED, "LED/Buzzer error: " + e.getMessage());
        }
    }
}
