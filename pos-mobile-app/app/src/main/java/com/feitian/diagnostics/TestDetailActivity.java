package com.feitian.diagnostics;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.feitian.diagnostics.diagnostics.DiagnosticResult;
import com.feitian.diagnostics.diagnostics.DiagnosticStatus;
import com.feitian.diagnostics.diagnostics.hardware.PrinterHelper;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;

public class TestDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_detail);

        // Get Data from Intent
        String name = getIntent().getStringExtra("test_name");
        String status = getIntent().getStringExtra("test_status");
        String details = getIntent().getStringExtra("test_details");

        // Bind Views
        TextView tvName = findViewById(R.id.testName);
        TextView tvStatus = findViewById(R.id.testStatus);
        TextView tvDetails = findViewById(R.id.testDetails);
        ImageView ivIcon = findViewById(R.id.testIcon);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);
        MaterialButton btnPrintTestReceipt = findViewById(R.id.btnPrintTestReceipt);

        tvName.setText(name);
        tvStatus.setText(status);
        tvDetails.setText(details);

        // Logic: Only show Print button for the Printer Test
        if ("Printer Test".equalsIgnoreCase(name)) {
            btnPrintTestReceipt.setVisibility(View.VISIBLE);
            btnPrintTestReceipt.setOnClickListener(v -> {
                // Generate a mini receipt for just this test
                DiagnosticResult singleResult = new DiagnosticResult(name, DiagnosticStatus.valueOf(status), details);
                PrinterHelper.printReceipt(getApplicationContext(), Collections.singletonList(singleResult), "Single Test");
            });
        } else {
            btnPrintTestReceipt.setVisibility(View.GONE);
        }

        // Style based on status
        int color;
        if ("PASS".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, R.color.status_pass);
        } else if ("FAIL".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, R.color.status_fail);
        } else if ("WARNING".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, R.color.status_warning);
        } else {
            color = ContextCompat.getColor(this, R.color.status_skipped);
        }

        tvStatus.setTextColor(color);
        ivIcon.setColorFilter(color);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> finish());
    }
}
