package com.example.speechtotext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;
    private Button btnClear;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    private static final int PERMISSION_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupButtons();
        checkPermissions();
        setupSpeechRecognizer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Автоматически запускаем стенографирование при запуске программы
        // после проверки разрешений
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED && speechRecognizer != null) {
            startAutoTranscription();
        }
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
                startTranscription();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTranscription();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearText();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_RECORD_AUDIO
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на запись получено", Toast.LENGTH_SHORT).show();
                // Автоматически запускаем стенографирование после получения разрешений
                startAutoTranscription();
            } else {
                Toast.makeText(this, "Для работы приложения требуется разрешение на запись", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Статус: Говорите...");
                            // Toast.makeText(MainActivity.this, "Говорите...", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onBeginningOfSpeech() {
                    // Начало речи
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Уровень громкости изменился
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Получен буфер
                }

                @Override
                public void onEndOfSpeech() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Статус: Обработка...");
                        }
                    });
                }

                @Override
                public void onError(int error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String errorMessage = getErrorText(error);
                            tvStatus.setText("Статус: Ошибка - " + errorMessage);

                            // Автоматически перезапускаем прослушивание при некоторых ошибках
                            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                if (isListening) {
                                    startListening();
                                }
                            }

                            // Логика для ошибки сети: останавливаем и снова запускаем стенографирование
                            if (error == SpeechRecognizer.ERROR_NETWORK ||
                                    error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                                if (isListening) {
                                    // Сначала останавливаем
                                    stopTranscription();

                                    // Задержка перед повторным запуском (например, 1 секунда)
                                    new android.os.Handler().postDelayed(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    // Снова запускаем
                                                    startTranscription();
                                                    Toast.makeText(MainActivity.this,
                                                            "Перезапуск после ошибки сети",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            },
                                            1000 // 1 секунда задержки
                                    );
                                }
                            }
                        }
                    });
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        appendToResult(text);
                        appendToResult(".\r\n");
                    }

                    // Перезапускаем прослушивание, если режим активен
                    if (isListening) {
                        startListening();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Частичные результаты (не поддерживается всеми устройствами)
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Событие
                }
            });
        } else {
            Toast.makeText(this, "Распознавание речи не поддерживается на этом устройстве", Toast.LENGTH_LONG).show();
        }
    }

    // Метод для автоматического запуска при старте программы
    private void startAutoTranscription() {
        if (!isListening) {
            startTranscription();
        }
    }

    private void startTranscription() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            if (speechRecognizer != null) {
                isListening = true;
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                tvStatus.setText("Статус: Запуск...");
                startListening();
            } else {
                Toast.makeText(this, "Распознаватель речи не инициализирован", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Требуется разрешение на запись аудио", Toast.LENGTH_SHORT).show();
            checkPermissions();
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU"); // Русский язык
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);
        }
    }

    private void stopTranscription() {
        isListening = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }

        tvStatus.setText("Статус: Остановлено");
        Toast.makeText(this, "Стенографирование остановлено", Toast.LENGTH_SHORT).show();
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

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Ошибка аудио";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Ошибка клиента";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Недостаточно прав";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Ошибка сети";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Таймаут сети";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Не найдено совпадений";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Распознаватель занят";
            case SpeechRecognizer.ERROR_SERVER:
                return "Ошибка сервера";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Таймаут речи";
            default:
                return "Неизвестная ошибка: " + errorCode;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}