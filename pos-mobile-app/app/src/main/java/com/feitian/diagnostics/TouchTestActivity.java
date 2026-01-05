package com.feitian.diagnostics;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.feitian.diagnostics.diagnostics.TouchscreenTestV2;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TouchTestActivity extends AppCompatActivity {

    private TextView timerText;
    private CountDownTimer countDownTimer;
    private MaterialButton btnFinish;
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_test);

        timerText = findViewById(R.id.touchTimerText);
        btnFinish = findViewById(R.id.btnFinishTouch);
        drawingView = findViewById(R.id.drawingView);
        MaterialButton btnClear = findViewById(R.id.btnClearCanvas);

        btnFinish.setOnClickListener(v -> {
            TouchscreenTestV2.registerTouch();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.clear();
            }
        });

        startTimer();
    }

    private void startTimer() {
        // 30 seconds duration
        countDownTimer = new CountDownTimer(30000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format(Locale.US, "%.1fs", millisUntilFinished / 1000.0));
                
                // Show finish button after 3 seconds of drawing to allow manual completion
                if (millisUntilFinished < 27000) {
                    btnFinish.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFinish() {
                timerText.setText(R.string.countdown_zero);
                finish(); 
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
