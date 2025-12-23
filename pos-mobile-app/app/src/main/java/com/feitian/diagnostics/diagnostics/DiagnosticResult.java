package com.feitian.diagnostics.diagnostics;

import androidx.annotation.NonNull;

public class DiagnosticResult {

    private final String name;
    private final DiagnosticStatus status;
    private final String details;

    public DiagnosticResult(String name, DiagnosticStatus status, String details) {
        this.name = name;
        this.status = status;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public DiagnosticStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    @NonNull
    @Override
    public String toString() {
        return name + ": " + status + "\n" + details + "\n";
    }
}
