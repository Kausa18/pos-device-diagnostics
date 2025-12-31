package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import com.ftpos.library.smartpos.printer.PrintStatus;
import com.ftpos.library.smartpos.printer.Printer;
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
        try {
            Printer printer = Printer.getInstance(context);
            if (printer == null) {
                return new DiagnosticResult(
                    getName(),
                    DiagnosticStatus.SKIPPED,
                    "Printer SDK not present or service not bound"
                );
            }

            PrintStatus printStatus = new PrintStatus();
            int status = printer.getStatus(printStatus);

            if (status == 0) {
                return new DiagnosticResult(getName(), DiagnosticStatus.PASS, "Printer ready (status=0)");
            } else if (status == 240) {
                return new DiagnosticResult(getName(), DiagnosticStatus.WARNING, "Printer out of paper (code=240)");
            } else {
                return new DiagnosticResult(getName(), DiagnosticStatus.FAIL, "Printer error code: " + status);
            }

        } catch (NoClassDefFoundError | Exception e) {
            return new DiagnosticResult(
                getName(),
                DiagnosticStatus.FAIL,
                "Printer SDK error: " + e.getMessage()
            );
        }
    }
}
