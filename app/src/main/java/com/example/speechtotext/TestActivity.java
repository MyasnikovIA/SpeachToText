package com.example.speechtotext;

import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;
    private Button btnClear;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    private static final int PERMISSION_RECORD_AUDIO = 1;

    private SpeechToText speechToText ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupButtons();
        // checkPermissions();
        // setupSpeechRecognizer();
        speechToText=new SpeechToText(this);
        speechToText.checkPermissions();
        speechToText.onStatus((String status)->{
            setText(status);
        });
        speechToText.onResults((String txt)->{
            appendToResult(txt);
        });
        speechToText.setupSpeechRecognizer();
        speechToText.startTranscription();
    }

    private void initViews() {
        tvResult = findViewById(R.id.tvResult);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnClear = findViewById(R.id.btnClear);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speechToText.startTranscription();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speechToText.stopTranscription();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearText();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        speechToText.onRequestPermissionsResult( requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechToText != null) {
            speechToText.onDestroy();
        }
    }


    private void clearText() {
        tvResult.setText("Текст появится здесь...");
        Toast.makeText(this, "Текст очищен", Toast.LENGTH_SHORT).show();
    }

    private void appendToResult(String newText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentText = tvResult.getText().toString();

                if (currentText.equals("Текст появится здесь...")) {
                    tvResult.setText(newText);
                } else {
                    tvResult.setText(currentText + " " + newText);
                }

                tvStatus.setText("Статус: Получен текст");
            }
        });
    }
    private void setText(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText(msg);
            }
        });
    }
}