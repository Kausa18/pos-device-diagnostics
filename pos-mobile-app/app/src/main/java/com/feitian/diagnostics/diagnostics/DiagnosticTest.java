package com.feitian.diagnostics.diagnostics;

import android.content.Context;

public interface DiagnosticTest {
    String getName();
    DiagnosticResult run(Context context);
}
