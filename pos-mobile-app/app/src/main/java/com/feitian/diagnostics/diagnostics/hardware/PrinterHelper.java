package com.feitian.diagnostics.diagnostics.hardware;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.ftpos.library.smartpos.printer.OnPrinterCallback;
import com.ftpos.library.smartpos.printer.Printer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrinterHelper {

    private static final String TAG = "PrinterHelper";

    public static void printReceipt(Context context, List<DiagnosticResult> results, String terminalId) {
        Printer printer = Printer.getInstance(context);
        if (printer == null) {
            Log.e(TAG, "Printer instance unavailable");
            return;
        }

        try {
            // 1. Open printer
            int ret = printer.open();
            if (ret != 0) {
                Log.e(TAG, "Failed to open printer: " + ret);
                return;
            }

            // 2. Start caching (Required by some FTSDK versions for batch printing)
            printer.startCaching();

            // 3. Header
            printer.setAlignStyle(1); // Center
            Bundle fontBold = new Bundle();
            fontBold.putInt("size", 30);
            fontBold.putBoolean("bold", true);
            printer.setFont(fontBold);
            printer.printStr("FEITIAN DIAGNOSTICS\n");
            
            printer.setAlignStyle(0); // Left
            Bundle fontSmall = new Bundle();
            fontSmall.putInt("size", 24); // standard size
            printer.setFont(fontSmall);
            
            printer.printStr("================================\n");
            printer.printStr("Terminal ID: " + terminalId + "\n");
            printer.printStr("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
            printer.printStr("--------------------------------\n");
            
            // 4. Results
            for (DiagnosticResult result : results) {
                String name = result.getName();
                String status = result.getStatus().name();
                // Simpler format to ensure compatibility
                printer.printStr(name + ": [" + status + "]\n");
            }
            
            printer.printStr("================================\n");
            printer.setAlignStyle(1); // Center
            printer.printStr("\n      DIAGNOSTICS COMPLETE\n\n\n\n\n");
            
            // 5. Finalize print
            printer.print(new OnPrinterCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Receipt printed successfully");
                    printer.close();
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "Print failed: " + error);
                    printer.close();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Printer error: " + e.getMessage());
        }
    }
}
