package com.example.speechtotext;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class SpeechToText {

    public interface CallbackType {
        public abstract void  setText(String txt);
    }


    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private Activity activity;
    private Context context;
    private CallbackType onResultsFunction=null;
    private CallbackType onStatusFunction=null;

    private static final int PERMISSION_RECORD_AUDIO = 12421;

    public SpeechToText(Activity activity){
        this.activity = activity;
        this.context = this.activity.getBaseContext();
    }
    public void onResults(CallbackType onResultsFunction){
        this.onResultsFunction = onResultsFunction;
    }
    public void onStatus(CallbackType onStatusFunction){
        this.onStatusFunction = onStatusFunction;
    }



    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.RECORD_AUDIO},PERMISSION_RECORD_AUDIO);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Разрешение на запись получено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Для работы приложения требуется разрешение на запись", Toast.LENGTH_LONG).show();
            }
        }
    }
    public void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (onStatusFunction!=null) {
                                onStatusFunction.setText("Статус: Говорите...");
                            }
                            //Toast.makeText(MainActivity.this, "Говорите...", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onBeginningOfSpeech() {
                    // Начало речи
                    if (onStatusFunction!=null) {
                        onStatusFunction.setText("Статус: Начало речи...");
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Уровень громкости изменился
                    // setText("Статус: Уровень громкости изменился...");
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Получен буфер
                    if (onStatusFunction!=null) {
                        onStatusFunction.setText("Статус: Получен буфер...");
                    }
                }

                @Override
                public void onEndOfSpeech() {
                    if (onStatusFunction!=null) {
                        onStatusFunction.setText("Статус: Обработка...");
                    }
                   //runOnUiThread(new Runnable() {
                   //    @Override
                   //    public void run() {
                   //        setText("Статус: Обработка...");
                   //    }
                   //});
                }

                @Override
                public void onError(int error) {
                    if (onStatusFunction!=null) {
                        String errorMessage = getErrorText(error);
                        onStatusFunction.setText("Статус: Ошибка - " + errorMessage);;
                    }
                   //runOnUiThread(new Runnable() {
                   //    @Override
                   //    public void run() {
                   //        String errorMessage = getErrorText(error);
                   //        tvStatus.setText("Статус: Ошибка - " + errorMessage);

                   //        // Автоматически перезапускаем прослушивание при некоторых ошибках
                   //        if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                   //                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                   //            if (isListening) {
                   //                startListening();
                   //            }
                   //        }
                   //    }
                   //});
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        text += ".\r\n";
                        if (onResultsFunction!=null) {
                            onResultsFunction.setText(text);
                        }
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
                //private void setText(String msg) {
                //    runOnUiThread(new Runnable() {
                //        @Override
                //        public void run() {
                //            tvStatus.setText(msg);
                //        }
                //    });
                //}
            });
        } else {
            Toast.makeText(context, "Распознавание речи не поддерживается на этом устройстве", Toast.LENGTH_LONG).show();
        }
    }

    public void startTranscription() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (speechRecognizer != null) {
                isListening = true;
                startListening();
            } else {
                Toast.makeText(context, "Распознаватель речи не инициализирован", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Требуется разрешение на запись аудио", Toast.LENGTH_SHORT).show();
            checkPermissions();
        }
    }

    public void stopTranscription() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
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


    protected void onDestroy() {
        //super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
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
}
