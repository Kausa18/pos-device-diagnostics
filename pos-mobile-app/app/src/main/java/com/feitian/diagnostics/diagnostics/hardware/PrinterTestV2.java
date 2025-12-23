package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.DiagnosticTest;

public class PrinterTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Printer Test";
    }

    @Override
    public DiagnosticResult run(Context context) {
        return new DiagnosticResult(
                getName(),
                DiagnosticStatus.SKIPPED,
                "Printer SDK not integrated on this device."
        );
    }
}
