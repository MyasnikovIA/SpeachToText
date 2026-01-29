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
    private static final long ERROR_RETRY_DELAY = 1000; // 1 секунда
    private static final long SPEECH_TIMEOUT_DELAY = 3000; // 3 секунды для таймаута речи

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
                        }
                    });
                }

                @Override
                public void onBeginningOfSpeech() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Статус: Распознавание...");
                        }
                    });
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Можно использовать для визуализации громкости
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Не используется
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

                            // Автоматически перезапускаем прослушивание при большинстве ошибок
                            if (isListening) {
                                boolean shouldRestart = false;

                                switch (error) {
                                    case SpeechRecognizer.ERROR_NO_MATCH:
                                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                    case SpeechRecognizer.ERROR_CLIENT:
                                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                        shouldRestart = true;
                                        break;

                                    case SpeechRecognizer.ERROR_NETWORK:
                                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                        shouldRestart = true;
                                        Toast.makeText(MainActivity.this,
                                                "Ошибка сети, перезапуск...",
                                                Toast.LENGTH_SHORT).show();
                                        break;

                                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                        checkPermissions();
                                        break;
                                }

                                if (shouldRestart) {
                                    // Задержка перед повторным запуском
                                    new android.os.Handler().postDelayed(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isListening) {
                                                        startListening();
                                                    }
                                                }
                                            },
                                            ERROR_RETRY_DELAY
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
                        if (text != null && !text.trim().isEmpty()) {
                            appendToResult(text);
                            // Добавляем пробел для разделения предложений
                            appendToResult(" ");
                        }
                    }

                    // НЕМЕДЛЕННО перезапускаем прослушивание без задержки
                    if (isListening) {
                        // Используем post для немедленного запуска
                        new android.os.Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                if (isListening) {
                                    startListening();
                                }
                            }
                        });
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Используем частичные результаты для более плавного отображения
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        if (partialText != null && !partialText.trim().isEmpty()) {
                            // Можно обновлять статус или показывать частичный текст
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvStatus.setText("Статус: " + partialText.substring(0, Math.min(partialText.length(), 30)) + "...");
                                }
                            });
                        }
                    }
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

                // Начинаем прослушивание с оптимальными параметрами для непрерывности
                startContinuousListening();
            } else {
                Toast.makeText(this, "Распознаватель речи не инициализирован", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Требуется разрешение на запись аудио", Toast.LENGTH_SHORT).show();
            checkPermissions();
        }
    }

    private void startContinuousListening() {
        if (speechRecognizer != null && isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");

            // Ключевые параметры для непрерывного распознавания
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            // Увеличиваем время ожидания речи
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000); // 5 секунд
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3 секунды
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000); // 2 секунды

            // Пытаемся использовать веб-сервис Google для лучшего качества
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);

            speechRecognizer.startListening(intent);
        }
    }

    private void startListening() {
        startContinuousListening();
    }

    private void stopTranscription() {
        isListening = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                    tvResult.setText(newText.trim());
                } else {
                    // Убираем лишние пробелы в начале нового текста
                    String trimmedNewText = newText.trim();

                    // Добавляем пробел только если предыдущий текст не заканчивается знаком препинания
                    if (!currentText.isEmpty() &&
                            !currentText.endsWith(" ") &&
                            !currentText.endsWith(".") &&
                            !currentText.endsWith("!") &&
                            !currentText.endsWith("?")) {
                        currentText += " ";
                    }

                    tvResult.setText(currentText + trimmedNewText);
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