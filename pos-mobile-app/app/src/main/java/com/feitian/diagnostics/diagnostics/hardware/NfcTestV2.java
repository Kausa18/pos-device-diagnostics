package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;

public class NfcTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "NFC Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        return new DiagnosticResult(
                getName(),
                DiagnosticStatus.SKIPPED,
                "NFC hardware access requires vendor SDK."
        );
    }
}
