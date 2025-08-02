package com.example.vocabulary;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class PracticeActivity extends AppCompatActivity {

    private TextView questionText, learnWord, learnMeaning, speakText;
    private EditText answerInput;
    private RadioGroup choicesGroup;
    private Button listenButton, speakButton, okButton;

    private LinearLayout fillInLayout, multipleChoiceLayout, pronunciationLayout, learnLayout;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private ArrayList<VocabularyItem> vocabList;
    private ArrayList<QuestionItem> questionList;
    private int questionIndex = 0;

    private QuestionItem currentQuestion;
    private VocabularyItem currentWord;
    private int questionType = 0;

    private TextToSpeech tts;
    private int topicId = -1;
    private boolean hasLearned = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private static final String PREFS_NAME = "settings_prefs";
    private static final String KEY_TTS_SLOW = "tts_slow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        questionText = findViewById(R.id.questionText);
        learnWord = findViewById(R.id.learnWord);
        learnMeaning = findViewById(R.id.learnMeaning);
        answerInput = findViewById(R.id.answerInput);
        choicesGroup = findViewById(R.id.choicesGroup);
        listenButton = findViewById(R.id.listenButton);
        speakButton = findViewById(R.id.speakButton);
        okButton = findViewById(R.id.okButton);
        speakText = findViewById(R.id.speakText);

        Button learnListenButton = findViewById(R.id.learnListenButton);
        Button learnOkButton = findViewById(R.id.learnOkButton);

        fillInLayout = findViewById(R.id.fillInLayout);
        multipleChoiceLayout = findViewById(R.id.multipleChoiceLayout);
        pronunciationLayout = findViewById(R.id.pronunciationLayout);
        learnLayout = findViewById(R.id.learnLayout);

        topicId = getIntent().getIntExtra("topic_id", -1);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                applyTtsRate();
            }
        });

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        loadVocabulary();
        showNextQuestion();

        okButton.setOnClickListener(v -> checkAnswer());

        learnOkButton.setOnClickListener(v -> {
            hasLearned = true;
            showQuestionLayout();
        });

        learnListenButton.setOnClickListener(v -> {
            if (currentWord != null && tts != null) {
                applyTtsRate();
                tts.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        listenButton.setOnClickListener(v -> {
            if (currentWord != null && tts != null) {
                applyTtsRate();
                tts.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        speakButton.setOnClickListener(v -> Toast.makeText(this, "Giữ nút để ghi âm", Toast.LENGTH_SHORT).show());

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO: message = "Lỗi âm thanh"; break;
                    case SpeechRecognizer.ERROR_CLIENT: message = "Lỗi ứng dụng"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Thiếu quyền"; break;
                    case SpeechRecognizer.ERROR_NETWORK: message = "Lỗi mạng"; break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Hết thời gian mạng"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH: message = "Không khớp kết quả"; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "Đang bận"; break;
                    case SpeechRecognizer.ERROR_SERVER: message = "Lỗi server"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "Không phát hiện giọng nói"; break;
                    default: message = "Lỗi không xác định"; break;
                }
                Toast.makeText(PracticeActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0).trim().toLowerCase();
                    String targetWord = currentWord.word.trim().toLowerCase();

                    speakText.setText(spokenText);

                    if (spokenText.contains(targetWord)) {
                        Toast.makeText(PracticeActivity.this, "Phát âm đúng!", Toast.LENGTH_SHORT).show();
                        ContentValues values = new ContentValues();
                        values.put("status", 1);
                        db.update("Vocabulary", values, "id = ?", new String[]{String.valueOf(currentWord.id)});
                        showNextQuestion();
                    } else {
                        Toast.makeText(PracticeActivity.this, "Chưa đúng, bạn nói: " + spokenText, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speakButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
                        return true;
                    }
                    Toast.makeText(this, "Bắt đầu nói...", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        speechRecognizer.startListening(speechIntent);
                    }, 400);
                    return true;
                case MotionEvent.ACTION_UP:
                    Toast.makeText(this, "Dừng ghi âm", Toast.LENGTH_SHORT).show();
                    speechRecognizer.stopListening();
                    return true;
            }
            return false;
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void applyTtsRate() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isSlow = prefs.getBoolean(KEY_TTS_SLOW, false);
        tts.setSpeechRate(isSlow ? 0.5f : 1.0f);
    }

    private void loadVocabulary() {
        vocabList = new ArrayList<>();
        questionList = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM Vocabulary WHERE topic_id = ?", new String[]{String.valueOf(topicId)});
        while (c.moveToNext()) {
            VocabularyItem item = new VocabularyItem();
            item.id = c.getInt(c.getColumnIndexOrThrow("id"));
            item.word = c.getString(c.getColumnIndexOrThrow("word"));
            item.meaning = c.getString(c.getColumnIndexOrThrow("meaning_vi"));
            vocabList.add(item);

            questionList.add(new QuestionItem(item, 0));
            questionList.add(new QuestionItem(item, 1));
            questionList.add(new QuestionItem(item, 2));
        }
        c.close();
        Collections.shuffle(questionList);
    }

    private void showNextQuestion() {
        if (questionIndex >= questionList.size()) {
            Toast.makeText(this, "Hoàn thành tất cả câu hỏi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentQuestion = questionList.get(questionIndex++);
        currentWord = currentQuestion.vocab;
        questionType = currentQuestion.type;
        hasLearned = false;

        fillInLayout.setVisibility(View.GONE);
        multipleChoiceLayout.setVisibility(View.GONE);
        pronunciationLayout.setVisibility(View.GONE);
        questionText.setVisibility(View.GONE);
        okButton.setVisibility(View.GONE);

        learnLayout.setVisibility(View.VISIBLE);
        learnWord.setText(currentWord.word);
        learnMeaning.setText(currentWord.meaning);
    }

    private void showQuestionLayout() {
        learnLayout.setVisibility(View.GONE);
        questionText.setVisibility(View.VISIBLE);
        okButton.setVisibility(View.VISIBLE);

        switch (questionType) {
            case 0:
                fillInLayout.setVisibility(View.VISIBLE);
                questionText.setText("Nghĩa tiếng Việt: " + currentWord.meaning);
                answerInput.setText("");
                break;
            case 1:
                multipleChoiceLayout.setVisibility(View.VISIBLE);
                questionText.setText("Chọn nghĩa đúng của từ: " + currentWord.word);
                setupChoices();
                break;
            case 2:
                pronunciationLayout.setVisibility(View.VISIBLE);
                questionText.setText("Phát âm từ: " + currentWord.word);
                speakText.setText("");
                break;
        }
    }

    private void setupChoices() {
        ArrayList<String> options = new ArrayList<>();
        options.add(currentWord.meaning);

        for (VocabularyItem item : vocabList) {
            if (!item.word.equals(currentWord.word) && options.size() < 4) {
                options.add(item.meaning);
            }
        }

        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            RadioButton rb = (RadioButton) choicesGroup.getChildAt(i);
            rb.setText(i < options.size() ? options.get(i) : "");
        }

        choicesGroup.clearCheck();
    }

    private void checkAnswer() {
        boolean correct = false;

        switch (questionType) {
            case 0:
                String userInput = answerInput.getText().toString().trim();
                correct = userInput.equalsIgnoreCase(currentWord.word);
                break;
            case 1:
                int selectedId = choicesGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selected = findViewById(selectedId);
                    correct = selected.getText().toString().equals(currentWord.meaning);
                }
                break;
            case 2:
                correct = true;
                break;
        }

        if (correct) {
            ContentValues values = new ContentValues();
            values.put("status", 1);
            db.update("Vocabulary", values, "id = ?", new String[]{String.valueOf(currentWord.id)});
        }

        Toast.makeText(this, correct ? "Đúng!" : "Sai!", Toast.LENGTH_SHORT).show();
        showNextQuestion();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    static class VocabularyItem {
        int id;
        String word;
        String meaning;
    }

    static class QuestionItem {
        VocabularyItem vocab;
        int type;

        public QuestionItem(VocabularyItem vocab, int type) {
            this.vocab = vocab;
            this.type = type;
        }
    }
}
