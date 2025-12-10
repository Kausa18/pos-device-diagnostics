package com.feitian.diagnostics;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView statusLabel;
    private TextView outputLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnRunDiagnostics = findViewById(R.id.btnRunDiagnostics);
        statusLabel = findViewById(R.id.statusLabel);
        outputLog = findViewById(R.id.outputLog);

        btnRunDiagnostics.setOnClickListener(v -> runDiagnostics());
    }

    private void runDiagnostics() {
        statusLabel.setText(R.string.status_running_diagnostics);
        outputLog.setText(R.string.starting_diagnostics);

        // TODO: Implement your diagnostic tests here.
        // For demonstration, I'll just append some text.
        outputLog.append(getString(R.string.test_1_passed));
        outputLog.append(getString(R.string.test_2_passed));

        statusLabel.setText(R.string.status_diagnostics_complete);
    }
}
