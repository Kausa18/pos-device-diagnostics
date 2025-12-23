package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;

public class CardReaderTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Card Reader Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        return new DiagnosticResult(
                getName(),
                DiagnosticStatus.SKIPPED,
                "Card reader hardware locked by POS firmware."
        );
    }
}
