package com.example.speechtotext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvStatus;
    private EditText etServerUrl;
    private Button btnStart;
    private Button btnStop;
    private Button btnClear;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private ExecutorService executorService;

    private static final int PERMISSION_RECORD_AUDIO = 1;
    private static final long ERROR_RETRY_DELAY = 1000;
    private static final long ERROR_AUTO_RESTART_DELAY = 2000;
    private static final int MAX_SERVER_ERROR_RETRIES = 999999;
    private int serverErrorRetryCount = 0;
    private boolean isRestarting = false;

    // Переменные для обработки знаков препинания
    private String lastProcessedText = "";
    private int consecutiveSilenceCount = 0;
    private static final int SILENCE_THRESHOLD_FOR_PERIOD = 3;

    // Handler для перезапуска при ошибках
    private Handler errorHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupButtons();
        checkPermissions();
        setupSpeechRecognizer();

        // Создаем пул потоков для отправки запросов
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && speechRecognizer != null) {
            startAutoTranscription();
        }
    }

    private void initViews() {
        tvResult = findViewById(R.id.tvResult);
        tvStatus = findViewById(R.id.tvStatus);
        etServerUrl = findViewById(R.id.etServerUrl);
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

    // Метод для отправки текста на сервер (аналогично Chrome плагину)
    private void sendTextToServer(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    // Получаем URL из поля ввода
                    String serverUrl = etServerUrl.getText().toString().trim();
                    if (serverUrl.isEmpty()) {
                        serverUrl = "192.168.15.3:8080"; // значение по умолчанию
                    }

                    // Добавляем протокол если отсутствует
                    if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                        serverUrl = "http://" + serverUrl;
                    }

                    // Подготавливаем текст для отправки (аналогично плагину)
                    String encodedText = URLEncoder.encode(text, "UTF-8");
                    String urlStr = serverUrl + "/?text=" + encodedText;

                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000); // 5 секунд таймаут
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("User-Agent", "Android-Speech-To-Text-App");
                    connection.setRequestProperty("Accept", "text/plain");

                    // Отправляем запрос
                    int responseCode = connection.getResponseCode();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (responseCode == 200 || responseCode == 204) {
                                // Успешно отправлено
                                tvStatus.setText("Статус: Текст отправлен на сервер");
                                serverErrorRetryCount = 0; // Сброс счетчика ошибок при успешной отправке
                            } else {
                                tvStatus.setText("Статус: Ошибка отправки (" + responseCode + ")");
                                // Не перезапускаем распознавание, просто сообщаем об ошибке отправки
                                Toast.makeText(MainActivity.this,
                                        "Ошибка отправки на сервер. Код: " + responseCode,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    // Читаем ответ (если есть)
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Статус: Ошибка соединения с сервером отправки");
                            // Не останавливаем распознавание, только показываем ошибку
                            Toast.makeText(MainActivity.this,
                                    "Ошибка отправки на сервер. Следующий текст попробуем отправить снова.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
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
                            tvStatus.setText("Статус:(" + error + ") Ошибка - " + errorMessage);

                            if (isListening && !isRestarting) {
                                boolean shouldRestart = false;
                                boolean shouldFullRestart = false; // Флаг для полного перезапуска

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

                                    case SpeechRecognizer.ERROR_SERVER:
                                        // Ошибка сервера распознавания - перезапускаем
                                        serverErrorRetryCount++;
                                        if (serverErrorRetryCount <= MAX_SERVER_ERROR_RETRIES) {
                                            shouldFullRestart = true; // Полный перезапуск для серверных ошибок
                                            Toast.makeText(MainActivity.this,
                                                    "Ошибка сервера распознавания (" + serverErrorRetryCount + "/" + MAX_SERVER_ERROR_RETRIES + "), полный перезапуск...",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Слишком много ошибок, останавливаем
                                            Toast.makeText(MainActivity.this,
                                                    "Слишком много ошибок сервера. Остановка распознавания.",
                                                    Toast.LENGTH_LONG).show();
                                            stopTranscription();
                                        }
                                        break;

                                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                        checkPermissions();
                                        break;

                                    case SpeechRecognizer.ERROR_CLIENT:
                                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                    case SpeechRecognizer.ERROR_AUDIO:
                                        // Эти ошибки требуют полного перезапуска
                                        shouldFullRestart = true;
                                        Toast.makeText(MainActivity.this,
                                                "Ошибка клиента/аудио, полный перезапуск...",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        shouldRestart = true;
                                        break;
                                }

                                if (shouldFullRestart) {
                                    // Полный перезапуск всей системы распознавания
                                    fullRestartRecognition();
                                } else if (shouldRestart && isListening) {
                                    // Обычный перезапуск прослушивания
                                    errorHandler.postDelayed(
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
                    // Сброс счетчика ошибок сервера при успешном распознавании
                    serverErrorRetryCount = 0;

                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        for (String text : matches) {
                            // Отправляем каждый распознанный фрагмент на сервер
                            if (text != null && !text.trim().isEmpty()) {
                                // Обрабатываем текст с пунктуацией
                                String processedText = processTextWithPunctuation(text);

                                // Добавляем в результат
                                appendToResult(processedText);
                                appendToResult("\n");

                                // Отправляем на сервер (аналогично Chrome плагину)
                                sendTextToServer(processedText);
                            }
                        }
                        appendToResult("\n----------------\n");
                    }

                    // Немедленно перезапускаем прослушивание
                    if (isListening) {
                        errorHandler.post(new Runnable() {
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

    // Новый метод для полного перезапуска системы распознавания
    private void fullRestartRecognition() {
        if (isRestarting) {
            return; // Уже в процессе перезапуска
        }

        isRestarting = true;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText("Статус: Полный перезапуск системы...");
            }
        });

        // 1. Останавливаем текущее распознавание
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Удаляем все pending задачи
        errorHandler.removeCallbacksAndMessages(null);

        // 3. Пауза перед повторной инициализацией
        errorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 4. Сбрасываем состояние
                consecutiveSilenceCount = 0;
                lastProcessedText = "";

                // 5. Создаем новый распознаватель
                if (SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);

                    // 6. Устанавливаем тот же самый listener
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
                                    consecutiveSilenceCount = 0;
                                }
                            });
                        }

                        @Override
                        public void onRmsChanged(float rmsdB) {
                            // Не используется
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
                                    tvStatus.setText("Статус:(" + error + ") Ошибка - " + errorMessage);

                                    if (isListening && !isRestarting) {
                                        boolean shouldRestart = false;
                                        boolean shouldFullRestart = false;

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

                                            case SpeechRecognizer.ERROR_SERVER:
                                                serverErrorRetryCount++;
                                                if (serverErrorRetryCount <= MAX_SERVER_ERROR_RETRIES) {
                                                    shouldFullRestart = true;
                                                    Toast.makeText(MainActivity.this,
                                                            "Ошибка сервера распознавания (" + serverErrorRetryCount + "/" + MAX_SERVER_ERROR_RETRIES + "), полный перезапуск...",
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this,
                                                            "Слишком много ошибок сервера. Остановка распознавания.",
                                                            Toast.LENGTH_LONG).show();
                                                    stopTranscription();
                                                }
                                                break;

                                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                                checkPermissions();
                                                break;

                                            case SpeechRecognizer.ERROR_CLIENT:
                                            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                            case SpeechRecognizer.ERROR_AUDIO:
                                                shouldFullRestart = true;
                                                Toast.makeText(MainActivity.this,
                                                        "Ошибка клиента/аудио, полный перезапуск...",
                                                        Toast.LENGTH_SHORT).show();
                                                break;
                                            default:
                                                shouldRestart = true;
                                                break;
                                        }

                                        if (shouldFullRestart) {
                                            fullRestartRecognition();
                                        } else if (shouldRestart && isListening) {
                                            errorHandler.postDelayed(
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
                            serverErrorRetryCount = 0;

                            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) {
                                for (String text : matches) {
                                    if (text != null && !text.trim().isEmpty()) {
                                        String processedText = processTextWithPunctuation(text);
                                        appendToResult(processedText);
                                        appendToResult("\n");
                                        sendTextToServer(processedText);
                                    }
                                }
                                appendToResult("\n----------------\n");
                            }

                            if (isListening) {
                                errorHandler.post(new Runnable() {
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

                    // 7. Запускаем прослушивание заново
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isListening) {
                                startListening();
                                tvStatus.setText("Статус: Система перезапущена, продолжаем...");
                                Toast.makeText(MainActivity.this, "Система распознавания перезапущена", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                // 8. Сбрасываем флаг перезапуска
                isRestarting = false;
            }
        }, ERROR_AUTO_RESTART_DELAY);
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
            // Автоматический запуск отключен
            // startTranscription();
        }
    }

    private void startTranscription() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (speechRecognizer != null) {
                isListening = true;
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                tvStatus.setText("Статус: Запуск...");
                serverErrorRetryCount = 0; // Сброс счетчика ошибок при ручном запуске
                isRestarting = false; // Сбрасываем флаг при ручном запуске
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
        isRestarting = false; // Сбрасываем флаг при остановке
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        // Удаляем все pending задачи из handler
        errorHandler.removeCallbacksAndMessages(null);

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
        tvResult.setText("");
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
                    tvResult.append(newText);
                }

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
        // Удаляем все pending задачи из handler
        errorHandler.removeCallbacksAndMessages(null);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}