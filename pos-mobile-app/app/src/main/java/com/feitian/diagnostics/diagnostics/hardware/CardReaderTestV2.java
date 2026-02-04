package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;
import com.ftpos.library.smartpos.icreader.IcReader;
import com.ftpos.library.smartpos.icreader.OnIcReaderCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CardReaderTestV2 {

    public static class IcReaderTest implements DiagnosticTest {
        @Override
        public String getName() {
            return "IC Card Test";
        }

        @Override
        public DiagnosticResult run(Context context) {
            final CountDownLatch latch = new CountDownLatch(1);
            final DiagnosticResult[] result = new DiagnosticResult[1];
            
            IcReader icReader = null;
            try {
                icReader = IcReader.getInstance(context);
                if (icReader == null) {
                    return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "IC Reader SDK unavailable");
                }

                icReader.openCard(10, new OnIcReaderCallback() {
                    @Override
                    public void onCardATR(byte[] bytes) {
                        result[0] = new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Card Inserted (ATR Detected)");
                        latch.countDown();
                    }

                    @Override
                    public void onError(int i) {
                        result[0] = new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "No card detected within 10 seconds. Please retry. (Error: " + i + ")");
                        latch.countDown();
                    }
                });

                // Correctly handling the result of await() to handle timeouts
                if (!latch.await(11, TimeUnit.SECONDS)) {
                    icReader.cancel();
                    return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "No card detected within 10 seconds. Please retry.");
                }
                
                return result[0];
            } catch (Exception e) {
                if (icReader != null) {
                    try { icReader.cancel(); } catch (Exception ignored) {}
                }
                return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "Error: " + e.getMessage());
            }
        }
    }

    public static String getIcPrompt() {
        return "\nIC Card Test:\nPlease INSERT a chip card now (10s timeout)...\n";
    }
}
