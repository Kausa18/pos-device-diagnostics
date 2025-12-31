package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;
import com.ftpos.library.smartpos.nfcreader.NfcReader;
import com.ftpos.library.smartpos.nfcreader.OnNfcReaderCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NfcTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "NFC Tap Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        final CountDownLatch latch = new CountDownLatch(1);
        final DiagnosticResult[] finalResult = new DiagnosticResult[1];

        try {
            NfcReader nfcReader = NfcReader.getInstance(context);
            if (nfcReader == null) {
                return new DiagnosticResult(getName(), DiagnosticStatus.SKIPPED, "NFC Service not bound. Test on real F20.");
            }

            // Powers on antenna and waits for a physical card tap
            nfcReader.openCard(10, new OnNfcReaderCallback() {
                @Override
                public void onCardATR(byte[] bytes) {
                    String details = "Success: Card Detected (ATR received)";
                    finalResult[0] = new DiagnosticResult(getName(), DiagnosticStatus.PASS, details);
                    latch.countDown();
                }

                @Override
                public void onError(int i) {
                    String errorMsg = (i == -536674300) ? "Timeout: No card tapped" : "NFC Hardware Error: " + i;
                    finalResult[0] = new DiagnosticResult(getName(), DiagnosticStatus.FAIL, errorMsg);
                    latch.countDown();
                }
            });

            if (!latch.await(12, TimeUnit.SECONDS)) {
                nfcReader.cancel();
                return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "NFC Test timed out");
            }

            return finalResult[0];

        } catch (Exception e) {
            return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "SDK Error: " + e.getMessage());
        }
    }

    public static String getPrompt() {
        return "\nNFC TAP TEST:\n>>> PLEASE TAP A BANK CARD ON THE SENSOR NOW (10s) <<<\n";
    }
}
