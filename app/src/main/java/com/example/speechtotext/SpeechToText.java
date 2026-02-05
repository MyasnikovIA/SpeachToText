package com.example.speechtotext;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import androidx.annotation.NonNull;
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


/*

package com.example.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvStatus;
    private EditText etServerUrl;
    private Button btnStart;
    private Button btnStop;
    private Button btnClear;

    private SpeechToText speechToText;

    private static final int PERMISSION_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupButtons();

        // Инициализация SpeechToText
        speechToText = new SpeechToText(this);
        speechToText.setupSpeechRecognizer();

        // Установка URL из EditText
        etServerUrl.setText("192.168.15.3:8080");

        // Настройка обратных вызовов
        setupCallbacks();

        checkPermissions();
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
                speechToText.resetState();
            }
        });
    }

    private void setupCallbacks() {
        // Callback для результатов распознавания
        speechToText.onResults(new SpeechToText.CallbackType() {
            @Override
            public void setText(String txt) {
                appendToResult(txt);
            }
        });

        // Callback для статуса
        speechToText.onStatus(new SpeechToText.StatusCallbackType() {
            @Override
            public void onStatusChanged(String status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Статус: " + status);
                    }
                });
            }
        });

        // Callback для ответа сервера
        speechToText.onServerResponse(new SpeechToText.ServerCallbackType() {
            @Override
            public void onServerResponse(String status, int responseCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Статус: " + status);
                        if (responseCode != 200 && responseCode != 204 && responseCode != -1) {
                            Toast.makeText(MainActivity.this,
                                    "Ошибка отправки на сервер. Код: " + responseCode,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
            } else {
                Toast.makeText(this, "Для работы приложения требуется разрешение на запись", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startTranscription() {
        // Устанавливаем URL сервера из EditText
        String serverUrl = etServerUrl.getText().toString().trim();
        if (!serverUrl.isEmpty()) {
            speechToText.setServerUrl(serverUrl);
        }

        speechToText.startTranscription();
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void stopTranscription() {
        speechToText.stopTranscription();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
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
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechToText != null) {
            speechToText.onDestroy();
        }
    }
}

 */

public class SpeechToText {

    public interface CallbackType {
        void setText(String txt);
    }

    public interface ServerCallbackType {
        void onServerResponse(String status, int responseCode);
    }

    public interface StatusCallbackType {
        void onStatusChanged(String status);
    }

    // Основные переменные
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private Activity activity;
    private Context context;
    private Handler errorHandler = new Handler();
    private ExecutorService executorService;

    // Обратные вызовы
    private CallbackType onResultsFunction = null;
    private StatusCallbackType onStatusFunction = null;
    private ServerCallbackType onServerResponseFunction = null;

    // Переменные для обработки знаков препинания
    private String lastProcessedText = "";
    private int consecutiveSilenceCount = 0;
    private static final int SILENCE_THRESHOLD_FOR_PERIOD = 3;
    private static final int MAX_SERVER_ERROR_RETRIES = 999999;
    private int serverErrorRetryCount = 0;

    // Настройки
    private String serverUrl = "192.168.15.3:8080";
    private String language = "ru-RU";
    private long errorRetryDelay = 1000;
    private boolean sendToServerEnabled = true;
    private boolean autoCapitalizationEnabled = true;
    private boolean punctuationProcessingEnabled = true;

    // Константы разрешений
    private static final int PERMISSION_RECORD_AUDIO = 12421;

    public SpeechToText(Activity activity) {
        this.activity = activity;
        this.context = this.activity.getBaseContext();
        this.executorService = Executors.newFixedThreadPool(2);
    }

    // Методы конфигурации
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setErrorRetryDelay(long delayMillis) {
        this.errorRetryDelay = delayMillis;
    }

    public void setSendToServerEnabled(boolean enabled) {
        this.sendToServerEnabled = enabled;
    }

    public void setAutoCapitalizationEnabled(boolean enabled) {
        this.autoCapitalizationEnabled = enabled;
    }

    public void setPunctuationProcessingEnabled(boolean enabled) {
        this.punctuationProcessingEnabled = enabled;
    }

    // Установка обратных вызовов
    public void onResults(CallbackType onResultsFunction) {
        this.onResultsFunction = onResultsFunction;
    }

    public void onStatus(StatusCallbackType onStatusFunction) {
        this.onStatusFunction = onStatusFunction;
    }

    public void onServerResponse(ServerCallbackType onServerResponseFunction) {
        this.onServerResponseFunction = onServerResponseFunction;
    }

    // Основные методы управления
    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
        } else {
            if (onStatusFunction != null) {
                onStatusFunction.onStatusChanged("Разрешение на запись уже предоставлено");
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Разрешение на запись получено", Toast.LENGTH_SHORT).show();
                if (onStatusFunction != null) {
                    onStatusFunction.onStatusChanged("Разрешение получено");
                }
            } else {
                Toast.makeText(context, "Для работы приложения требуется разрешение на запись", Toast.LENGTH_LONG).show();
                if (onStatusFunction != null) {
                    onStatusFunction.onStatusChanged("Разрешение отклонено");
                }
            }
        }
    }

    public void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    updateStatus("Говорите...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    updateStatus("Распознавание...");
                    consecutiveSilenceCount = 0; // Сброс счетчика тишины
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
                    updateStatus("Обработка...");
                    consecutiveSilenceCount++;
                }

                @Override
                public void onError(int error) {
                    handleRecognitionError(error);
                }

                @Override
                public void onResults(Bundle results) {
                    // Сброс счетчика ошибок сервера при успешном распознавании
                    serverErrorRetryCount = 0;

                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        for (String text : matches) {
                            if (text != null && !text.trim().isEmpty()) {
                                // Обрабатываем текст с пунктуацией
                                String processedText = punctuationProcessingEnabled ?
                                        processTextWithPunctuation(text) : text;

                                // Вызываем callback с результатом
                                if (onResultsFunction != null) {
                                    onResultsFunction.setText(processedText + "\n----------------\n");
                                }

                                // Отправляем на сервер если включено
                                if (sendToServerEnabled) {
                                    sendTextToServer(processedText);
                                }
                            }
                        }
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
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        if (partialText != null && !partialText.trim().isEmpty()) {
                            String displayText = partialText;
                            if (displayText.length() > 50) {
                                displayText = "..." + displayText.substring(displayText.length() - 47);
                            }
                            updateStatus("Частично: " + displayText);
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Событие
                }
            });
        } else {
            Toast.makeText(context, "Распознавание речи не поддерживается на этом устройстве", Toast.LENGTH_LONG).show();
            if (onStatusFunction != null) {
                onStatusFunction.onStatusChanged("Распознавание не поддерживается");
            }
        }
    }

    private void handleRecognitionError(int error) {
        String errorMessage = getErrorText(error);
        updateStatus("Ошибка (" + error + ") - " + errorMessage);

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
                    Toast.makeText(context, "Ошибка сети, перезапуск...", Toast.LENGTH_SHORT).show();
                    break;

                case SpeechRecognizer.ERROR_SERVER:
                    serverErrorRetryCount++;
                    if (serverErrorRetryCount <= MAX_SERVER_ERROR_RETRIES) {
                        shouldRestart = true;
                        Toast.makeText(context,
                                "Ошибка сервера распознавания (" + serverErrorRetryCount + "/" +
                                        MAX_SERVER_ERROR_RETRIES + "), перезапуск...",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context,
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
                    shouldRestart = true;
                    Toast.makeText(context, "Ошибка клиента, перезапуск...", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    shouldRestart = true;
                    break;
            }

            if (shouldRestart && isListening) {
                errorHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isListening) {
                            startListening();
                        }
                    }
                }, errorRetryDelay);
            }
        }
    }

    public void startTranscription() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (speechRecognizer != null) {
                isListening = true;
                serverErrorRetryCount = 0;
                updateStatus("Запуск...");
                startListening();
            } else {
                Toast.makeText(context, "Распознаватель речи не инициализирован", Toast.LENGTH_SHORT).show();
                if (onStatusFunction != null) {
                    onStatusFunction.onStatusChanged("Распознаватель не инициализирован");
                }
            }
        } else {
            Toast.makeText(context, "Требуется разрешение на запись аудио", Toast.LENGTH_SHORT).show();
            checkPermissions();
        }
    }

    public void stopTranscription() {
        isListening = false;
        errorHandler.removeCallbacksAndMessages(null);

        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        updateStatus("Остановлено");
        Toast.makeText(context, "Стенографирование остановлено", Toast.LENGTH_SHORT).show();
    }

    private void startListening() {
        if (speechRecognizer != null && isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);

            // Настройки для лучшего распознавания
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 8000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            intent.putExtra("android.speech.extra.DICTATION_MODE", true);

            speechRecognizer.startListening(intent);
        }
    }

    // Метод для отправки текста на сервер
    private void sendTextToServer(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String finalServerUrl = serverUrl;
                    if (!finalServerUrl.startsWith("http://") && !finalServerUrl.startsWith("https://")) {
                        finalServerUrl = "http://" + finalServerUrl;
                    }

                    String encodedText = URLEncoder.encode(text, "UTF-8");
                    String urlStr = finalServerUrl + "/?text=" + encodedText;

                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("User-Agent", "Android-Speech-To-Text-App");
                    connection.setRequestProperty("Accept", "text/plain");

                    int responseCode = connection.getResponseCode();

                    if (onServerResponseFunction != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (responseCode == 200 || responseCode == 204) {
                                    onServerResponseFunction.onServerResponse("Текст отправлен на сервер", responseCode);
                                    serverErrorRetryCount = 0;
                                } else {
                                    onServerResponseFunction.onServerResponse("Ошибка отправки (" + responseCode + ")", responseCode);
                                }
                            }
                        });
                    }

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
                    if (onServerResponseFunction != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onServerResponseFunction.onServerResponse("Ошибка соединения с сервером отправки", -1);
                            }
                        });
                    }
                    Toast.makeText(context,
                            "Ошибка отправки на сервер. Следующий текст попробуем отправить снова.",
                            Toast.LENGTH_SHORT).show();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    // Методы обработки пунктуации (аналогичные MainActivity)
    private String processTextWithPunctuation(String text) {
        String lowerText = text.toLowerCase().trim();
        String processedText = text;

        // 1. Добавляем точку в конец, если была длинная пауза
        if (consecutiveSilenceCount >= SILENCE_THRESHOLD_FOR_PERIOD) {
            if (!processedText.endsWith(".") && !processedText.endsWith("!") && !processedText.endsWith("?")) {
                processedText = processedText + ".";
            }
            consecutiveSilenceCount = 0;
        }

        // 2. Автоматическая капитализация начала предложения
        if (autoCapitalizationEnabled &&
                (lastProcessedText.isEmpty() ||
                        lastProcessedText.endsWith(".") ||
                        lastProcessedText.endsWith("!") ||
                        lastProcessedText.endsWith("?"))) {
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
        result = result.replaceAll("(?i)\\s*точка\\s*$", ".");
        result = result.replaceAll("(?i)\\s*запятая\\s*", ", ");
        result = result.replaceAll("(?i)\\s*восклицательный знак\\s*$", "!");
        result = result.replaceAll("(?i)\\s*вопросительный знак\\s*$", "?");
        result = result.replaceAll("(?i)\\s*двоеточие\\s*", ": ");
        result = result.replaceAll("(?i)\\s*точка с запятой\\s*", "; ");
        result = result.replaceAll("(?i)\\s*новый абзац\\s*", "\n\n");
        result = result.replaceAll("(?i)\\s*абзац\\s*", "\n\n");
        return result.trim();
    }

    private boolean isQuestion(String text) {
        String[] questionWords = {
                "кто", "что", "где", "когда", "почему", "зачем", "как",
                "сколько", "чей", "кого", "кому", "чем", "какой", "какая",
                "какое", "какие", "ли"
        };

        String lowerText = text.toLowerCase();
        for (String word : questionWords) {
            if (lowerText.startsWith(word + " ") ||
                    lowerText.contains(" " + word + " ") ||
                    lowerText.endsWith(" " + word)) {
                return true;
            }
        }

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
        StringBuilder result = new StringBuilder(text);
        String[] conjunctions = {"а", "но", "однако", "зато", "или", "либо", "то", "как"};
        for (String conj : conjunctions) {
            String pattern = "\\s" + conj + "\\s";
            String replacement = ", " + conj + " ";
            result = new StringBuilder(result.toString().replaceAll("(?i)" + pattern, replacement));
        }

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

        return result.toString();
    }

    // Вспомогательные методы
    private void updateStatus(final String status) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (onStatusFunction != null) {
                    onStatusFunction.onStatusChanged(status);
                }
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

    // Методы управления ресурсами
    public void onDestroy() {
        errorHandler.removeCallbacksAndMessages(null);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    // Методы для сброса состояния
    public void resetState() {
        lastProcessedText = "";
        consecutiveSilenceCount = 0;
        serverErrorRetryCount = 0;
    }

    // Методы для получения состояния
    public boolean isListening() {
        return isListening;
    }

    public int getServerErrorRetryCount() {
        return serverErrorRetryCount;
    }
}