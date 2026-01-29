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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;
    private Button btnClear;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    private static final int PERMISSION_RECORD_AUDIO = 1;
    private static final long ERROR_RETRY_DELAY = 1000;

    // Переменные для обработки знаков препинания
    private String lastProcessedText = "";
    private int consecutiveSilenceCount = 0;
    private static final int SILENCE_THRESHOLD_FOR_PERIOD = 3; // Количество тихих сессий для точки

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
                lastProcessedText = "";
                consecutiveSilenceCount = 0;
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
                            consecutiveSilenceCount = 0; // Сброс счетчика тишины
                        }
                    });
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Можно использовать для детектирования тишины
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
                            consecutiveSilenceCount++;
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

                            if (isListening) {
                                boolean shouldRestart = false;

                                switch (error) {
                                    case SpeechRecognizer.ERROR_NO_MATCH:
                                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                        shouldRestart = true;
                                        consecutiveSilenceCount++;
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

                                    default:
                                        shouldRestart = true;
                                        break;
                                }

                                if (shouldRestart) {
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
                            // Обрабатываем текст с добавлением знаков препинания
                            String processedText = processTextWithPunctuation(text);
                            appendToResult(processedText);
                        }
                    }

                    // Немедленно перезапускаем прослушивание
                    if (isListening) {
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
                    // Отображаем частичные результаты
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        if (partialText != null && !partialText.trim().isEmpty()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String displayText = partialText;
                                    if (displayText.length() > 50) {
                                        displayText = "..." + displayText.substring(displayText.length() - 47);
                                    }
                                    tvStatus.setText("Частично: " + displayText);
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

    private String processTextWithPunctuation(String text) {
        // Приводим к нижнему регистру для обработки
        String lowerText = text.toLowerCase().trim();

        // Обрабатываем текст и добавляем знаки препинания
        String processedText = text;

        // 1. Добавляем точку в конец, если была длинная пауза
        if (consecutiveSilenceCount >= SILENCE_THRESHOLD_FOR_PERIOD) {
            if (!processedText.endsWith(".") && !processedText.endsWith("!") && !processedText.endsWith("?")) {
                processedText = processedText + ".";
            }
            consecutiveSilenceCount = 0; // Сброс после добавления точки
        }

        // 2. Автоматическая капитализация начала предложения
        if (lastProcessedText.isEmpty() ||
                lastProcessedText.endsWith(".") ||
                lastProcessedText.endsWith("!") ||
                lastProcessedText.endsWith("?")) {
            if (processedText.length() > 0) {
                processedText = processedText.substring(0, 1).toUpperCase() +
                        processedText.substring(1);
            }
        }

        // 3. Обработка ключевых слов для знаков препинания
        processedText = addPunctuationByKeywords(processedText);

        // 4. Обработка вопросительных конструкций
        if (isQuestion(lowerText)) {
            if (!processedText.endsWith("?")) {
                // Убираем возможную точку в конце
                processedText = processedText.replaceAll("\\.$", "");
                processedText = processedText + "?";
            }
        }

        // 5. Обработка восклицательных конструкций
        if (isExclamation(lowerText)) {
            if (!processedText.endsWith("!")) {
                processedText = processedText.replaceAll("\\.$", "");
                processedText = processedText + "!";
            }
        }

        // 6. Добавление запятых по паузам и логике
        processedText = addCommasByPauses(processedText, lowerText);

        // Сохраняем последний обработанный текст
        lastProcessedText = processedText;

        return processedText;
    }

    private String addPunctuationByKeywords(String text) {
        String result = text;

        // Паттерны для замены с сохранением регистра
        result = result.replaceAll("(?i)\\s*точка\\s*$", ".");
        result = result.replaceAll("(?i)\\s*запятая\\s*", ", ");
        result = result.replaceAll("(?i)\\s*восклицательный знак\\s*$", "!");
        result = result.replaceAll("(?i)\\s*вопросительный знак\\s*$", "?");
        result = result.replaceAll("(?i)\\s*двоеточие\\s*", ": ");
        result = result.replaceAll("(?i)\\s*точка с запятой\\s*", "; ");

        // Замена словесных указаний на знаки
        result = result.replaceAll("(?i)\\s*новый абзац\\s*", "\n\n");
        result = result.replaceAll("(?i)\\s*абзац\\s*", "\n\n");

        return result.trim();
    }

    private boolean isQuestion(String text) {
        // Вопросительные слова
        String[] questionWords = {
                "кто", "что", "где", "когда", "почему", "зачем", "как",
                "сколько", "чей", "кого", "кому", "чем", "какой", "какая",
                "какое", "какие", "ли"
        };

        // Вопросительные конструкции
        String lowerText = text.toLowerCase();

        // Проверяем начало предложения
        for (String word : questionWords) {
            if (lowerText.startsWith(word + " ") ||
                    lowerText.contains(" " + word + " ") ||
                    lowerText.endsWith(" " + word)) {
                return true;
            }
        }

        // Проверяем интонацию (глагол в начале)
        String[] questionPatterns = {
                "^а\\s+", "^и\\s+", "^но\\s+", "^так\\s+", "^неужели\\s+",
                "^разве\\s+", "^что\\s+если", "^а\\s+что\\s+если"
        };

        for (String pattern : questionPatterns) {
            if (lowerText.matches(pattern + ".*")) {
                return true;
            }
        }

        return false;
    }

    private boolean isExclamation(String text) {
        String lowerText = text.toLowerCase();

        // Восклицательные слова и конструкции
        String[] exclamationWords = {
                "какой", "какая", "какое", "какие", "сколько", "так",
                "просто", "невероятно", "удивительно", "здорово", "круто",
                "отлично", "прекрасно", "восхитительно", "жутко", "страшно"
        };

        for (String word : exclamationWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private String addCommasByPauses(String text, String lowerText) {
        // Правила для добавления запятых
        StringBuilder result = new StringBuilder(text);

        // 1. Запятые перед союзами
        String[] conjunctions = {"а", "но", "однако", "зато", "или", "либо", "то", "как"};
        for (String conj : conjunctions) {
            String pattern = "\\s" + conj + "\\s";
            String replacement = ", " + conj + " ";
            result = new StringBuilder(result.toString().replaceAll("(?i)" + pattern, replacement));
        }

        // 2. Запятые после вводных слов
        String[] introWords = {"во-первых", "во-вторых", "в-третьих", "однако",
                "конечно", "возможно", "вероятно", "казалось",
                "кстати", "например", "итак", "следовательно"};
        for (String intro : introWords) {
            String pattern = "^" + intro + "\\s";
            String replacement = intro + ", ";
            result = new StringBuilder(result.toString().replaceAll("(?i)" + pattern, replacement));

            pattern = "\\s" + intro + "\\s";
            replacement = " " + intro + ", ";
            result = new StringBuilder(result.toString().replaceAll("(?i)" + pattern, replacement));
        }

        // 3. Запятые в перечислениях
        String[] enumerationWords = {"и", "или", "либо"};
        for (String enumWord : enumerationWords) {
            String pattern = "\\s\\w+\\s" + enumWord + "\\s\\w+";
            // Это упрощенная логика, можно сделать более сложную
        }

        return result.toString();
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

            // Настройки для лучшего распознавания
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            // Настройки времени ожидания
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 8000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);

            // Включаем детализацию
            intent.putExtra("android.speech.extra.DICTATION_MODE", true);

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
                    tvResult.setText(newText);
                } else {
                    // Умное добавление текста с учетом знаков препинания
                    String trimmedNewText = newText.trim();

                    // Если предыдущий текст заканчивается знаком препинания, добавляем пробел
                    if (!currentText.isEmpty() &&
                            !currentText.endsWith(" ") &&
                            !currentText.endsWith("\n")) {

                        // Проверяем последний символ
                        char lastChar = currentText.charAt(currentText.length() - 1);
                        if (lastChar != '.' && lastChar != '!' && lastChar != '?' &&
                                lastChar != ',' && lastChar != ':' && lastChar != ';') {
                            currentText += " ";
                        }
                    }

                    tvResult.setText(currentText + trimmedNewText);

                    // Прокручиваем ScrollView вниз
                    final ScrollView scrollView = findViewById(R.id.scrollView);
                    if (scrollView != null) {
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                }

                tvStatus.setText("Статус: Текст обновлен");
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