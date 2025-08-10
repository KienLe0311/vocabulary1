package com.example.vocabulary; // Thay thế bằng package của bạn

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout; // Giả sử bạn dùng LinearLayout cho các layout con

import androidx.annotation.NonNull;
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
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

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
            } else {
                Toast.makeText(PracticeActivity.this, "Không thể khởi tạo TextToSpeech", Toast.LENGTH_SHORT).show();
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
            if (currentWord != null && tts != null && tts.getEngines().size() > 0) {
                applyTtsRate();
                tts.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(PracticeActivity.this, "TTS chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        listenButton.setOnClickListener(v -> {
            if (currentWord != null && tts != null && tts.getEngines().size() > 0) {
                applyTtsRate();
                tts.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(PracticeActivity.this, "TTS chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        // Khởi tạo SpeechRecognizer và Intent
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // Đảm bảo ngôn ngữ này được hỗ trợ
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    // Sẵn sàng để nói
                }
                @Override public void onBeginningOfSpeech() {
                    // Bắt đầu nói
                }
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    // Kết thúc nói, đợi kết quả
                }

                @Override
                public void onError(int error) {
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO: message = "Lỗi âm thanh"; break;
                        case SpeechRecognizer.ERROR_CLIENT: message = "Lỗi ứng dụng (client)"; break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Thiếu quyền ghi âm"; break;
                        case SpeechRecognizer.ERROR_NETWORK: message = "Lỗi mạng"; break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Hết thời gian mạng"; break;
                        case SpeechRecognizer.ERROR_NO_MATCH: message = "Không khớp kết quả"; break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "Bộ nhận dạng đang bận"; break;
                        case SpeechRecognizer.ERROR_SERVER: message = "Lỗi server"; break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "Không phát hiện giọng nói"; break;
                        default: message = "Lỗi không xác định: " + error; break;
                    }
                    Toast.makeText(PracticeActivity.this, "Lỗi: " + message, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0).trim().toLowerCase(Locale.US);
                        String targetWord = currentWord.word.trim().toLowerCase(Locale.US);

                        speakText.setText(matches.get(0)); // Hiển thị kết quả gốc

                        if (spokenText.contains(targetWord)) {
                            Toast.makeText(PracticeActivity.this, "Phát âm đúng!", Toast.LENGTH_SHORT).show();
                            ContentValues values = new ContentValues();
                            values.put("status", 1); // Giả sử cột status tồn tại
                            db.update("Vocabulary", values, "id = ?", new String[]{String.valueOf(currentWord.id)});
                            showNextQuestion();
                        } else {
                            Toast.makeText(PracticeActivity.this, "Chưa đúng, bạn nói: " + spokenText, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(PracticeActivity.this, "Không nhận dạng được giọng nói.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận dạng giọng nói", Toast.LENGTH_LONG).show();
            speakButton.setEnabled(false); // Vô hiệu hóa nút nếu không hỗ trợ
        }


        speakButton.setOnTouchListener((v, event) -> {
            if (speechRecognizer == null) { // Kiểm tra nếu speechRecognizer chưa được khởi tạo
                Toast.makeText(this, "Không thể sử dụng tính năng nhận dạng giọng nói.", Toast.LENGTH_SHORT).show();
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startRecording();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                    }
                    return true; // Quan trọng: đã xử lý sự kiện
                case MotionEvent.ACTION_UP:
                    // Chỉ dừng ghi âm nếu quyền đã được cấp và đang ghi âm
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        stopRecording();
                    }
                    return true; // Quan trọng: đã xử lý sự kiện
            }
            return false;
        });
    }

    private void startRecording() {
        if (speechRecognizer != null) {
            speakText.setText("Đang nghe...");
            Toast.makeText(this, "Bắt đầu nói...", Toast.LENGTH_SHORT).show();
            speechRecognizer.startListening(speechIntent);
        }
    }

    private void stopRecording() {
        if (speechRecognizer != null) {
            Toast.makeText(this, "Dừng ghi âm", Toast.LENGTH_SHORT).show();
            speechRecognizer.stopListening(); // Sẽ kích hoạt onResults hoặc onError
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền ghi âm. Hãy nhấn giữ nút Nói để bắt đầu.", Toast.LENGTH_LONG).show();
                // Không tự động bắt đầu ghi âm ở đây, để người dùng nhấn lại nút
            } else {
                Toast.makeText(this, "Quyền ghi âm bị từ chối. Không thể sử dụng tính năng này.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void applyTtsRate() {
        if (tts == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isSlow = prefs.getBoolean(KEY_TTS_SLOW, false);
        tts.setSpeechRate(isSlow ? 0.5f : 1.0f);
    }

    private void loadVocabulary() {
        vocabList = new ArrayList<>();
        questionList = new ArrayList<>();
        // Đảm bảo topicId hợp lệ
        if (topicId == -1) {
            Toast.makeText(this, "Topic không hợp lệ", Toast.LENGTH_SHORT).show();
            finish(); // Kết thúc activity nếu không có topic
            return;
        }
        Cursor c = db.rawQuery("SELECT * FROM Vocabulary WHERE topic_id = ?", new String[]{String.valueOf(topicId)});
        if (c != null) {
            while (c.moveToNext()) {
                VocabularyItem item = new VocabularyItem();
                // Sử dụng getColumnIndex an toàn hơn
                int idCol = c.getColumnIndex("id");
                int wordCol = c.getColumnIndex("word");
                int meaningCol = c.getColumnIndex("meaning_vi"); // Đảm bảo tên cột chính xác

                if (idCol != -1) item.id = c.getInt(idCol);
                if (wordCol != -1) item.word = c.getString(wordCol);
                if (meaningCol != -1) item.meaning = c.getString(meaningCol);

                if (item.word != null && item.meaning != null) { // Chỉ thêm nếu dữ liệu hợp lệ
                    vocabList.add(item);
                    questionList.add(new QuestionItem(item, 0)); // Fill in
                    questionList.add(new QuestionItem(item, 1)); // Multiple choice
                    questionList.add(new QuestionItem(item, 2)); // Pronunciation
                }
            }
            c.close();
        }

        if (questionList.isEmpty()) {
            Toast.makeText(this, "Không có từ vựng nào cho topic này.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Collections.shuffle(questionList);
    }

    private void showNextQuestion() {
        if (questionIndex >= questionList.size()) {
            Toast.makeText(this, "Hoàn thành tất cả câu hỏi!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentQuestion = questionList.get(questionIndex++);
        currentWord = currentQuestion.vocab;
        questionType = currentQuestion.type;
        hasLearned = false; // Reset trạng thái đã học cho từ mới

        // Ẩn tất cả các layout câu hỏi trước
        fillInLayout.setVisibility(View.GONE);
        multipleChoiceLayout.setVisibility(View.GONE);
        pronunciationLayout.setVisibility(View.GONE);
        questionText.setVisibility(View.GONE);
        okButton.setVisibility(View.GONE);

        // Hiển thị layout học từ
        learnLayout.setVisibility(View.VISIBLE);
        if (currentWord != null) {
            learnWord.setText(currentWord.word);
            learnMeaning.setText(currentWord.meaning);
        } else {
            // Xử lý trường hợp currentWord là null (ít khi xảy ra nếu loadVocabulary đúng)
            Toast.makeText(this, "Lỗi tải từ vựng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showQuestionLayout() {
        learnLayout.setVisibility(View.GONE);
        questionText.setVisibility(View.VISIBLE);

        if (currentWord == null) {
            Toast.makeText(this, "Lỗi: Không có từ hiện tại.", Toast.LENGTH_SHORT).show();
            showNextQuestion(); // Chuyển sang câu hỏi tiếp theo nếu có lỗi
            return;
        }

        switch (questionType) {
            case 0: // Fill in the blank
                fillInLayout.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                questionText.setText("Nghĩa tiếng Việt: " + currentWord.meaning);
                answerInput.setText("");
                answerInput.requestFocus(); // Tự động focus vào ô nhập liệu
                break;
            case 1: // Multiple choice
                multipleChoiceLayout.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                questionText.setText("Chọn nghĩa đúng của từ: " + currentWord.word);
                setupChoices();
                break;
            case 2: // Pronunciation
                pronunciationLayout.setVisibility(View.VISIBLE);
                // Với pronunciation, không cần nút OK truyền thống, việc kiểm tra được xử lý trong onResults
                okButton.setVisibility(View.GONE); // Hoặc bạn có thể để nút OK để "Bỏ qua"
                questionText.setText("Phát âm từ: " + currentWord.word);
                speakText.setText(""); // Xóa kết quả phát âm trước đó
                break;
        }
    }

    private void setupChoices() {
        if (currentWord == null || vocabList == null || vocabList.isEmpty()) {
            return; // Không thể tạo lựa chọn nếu không có dữ liệu
        }
        ArrayList<String> options = new ArrayList<>();
        options.add(currentWord.meaning);

        // Lấy các nghĩa sai ngẫu nhiên từ danh sách từ vựng
        ArrayList<VocabularyItem> tempVocabList = new ArrayList<>(vocabList);
        tempVocabList.remove(currentWord); // Loại bỏ từ hiện tại để không chọn lại chính nó
        Collections.shuffle(tempVocabList);

        for (VocabularyItem item : tempVocabList) {
            if (options.size() < 4 && !item.meaning.equals(currentWord.meaning)) {
                options.add(item.meaning);
            }
            if (options.size() >=4) break;
        }
        // Nếu không đủ 4 lựa chọn, có thể thêm các lựa chọn giả hoặc xử lý khác
        while (options.size() < 4) {
            options.add("Nghĩa ngẫu nhiên " + options.size()); // Placeholder, cần cải thiện
        }


        Collections.shuffle(options);
        choicesGroup.clearCheck();

        for (int i = 0; i < choicesGroup.getChildCount(); i++) {
            View child = choicesGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                if (i < options.size()) {
                    rb.setText(options.get(i));
                    rb.setVisibility(View.VISIBLE);
                } else {
                    rb.setVisibility(View.GONE); // Ẩn nếu không đủ lựa chọn
                }
            }
        }
    }

    private void checkAnswer() {
        boolean correct = false;
        if (currentWord == null) {
            Toast.makeText(this, "Không có từ để kiểm tra", Toast.LENGTH_SHORT).show();
            showNextQuestion();
            return;
        }

        switch (questionType) {
            case 0: // Fill in
                String userInput = answerInput.getText().toString().trim();
                correct = userInput.equalsIgnoreCase(currentWord.word);
                break;
            case 1: // Multiple choice
                int selectedId = choicesGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selected = findViewById(selectedId);
                    correct = selected.getText().toString().equals(currentWord.meaning);
                } else {
                    Toast.makeText(this, "Bạn chưa chọn đáp án", Toast.LENGTH_SHORT).show();
                    return; // Không chuyển câu nếu chưa chọn
                }
                break;
            case 2: // Pronunciation - việc kiểm tra đã xảy ra trong onResults
                // Nếu bạn muốn nút OK cho pronunciation để "Bỏ qua" hoặc "Thử lại", bạn cần xử lý ở đây.
                // Hiện tại, nó sẽ tự động chuyển câu nếu phát âm đúng trong onResults.
                // Nếu không có hành động cụ thể cho nút OK ở đây, có thể không cần `correct = true`
                showNextQuestion(); // Chuyển câu hỏi nếu người dùng nhấn OK (ví dụ: bỏ qua)
                return; // Không cần xử lý Toast đúng/sai ở đây nữa
        }

        if (correct) {
            Toast.makeText(this, "Đúng!", Toast.LENGTH_SHORT).show();
            ContentValues values = new ContentValues();
            values.put("status", 1); // Giả sử cột status tồn tại và 1 là đã học
            db.update("Vocabulary", values, "id = ?", new String[]{String.valueOf(currentWord.id)});
        } else {
            Toast.makeText(this, "Sai!", Toast.LENGTH_SHORT).show();
        }
        showNextQuestion();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    // --- Lớp nội bộ VocabularyItem và QuestionItem ---
    // (Đảm bảo các lớp này được định nghĩa đúng cách trong dự án của bạn,
    // hoặc bạn có thể di chuyển chúng ra file riêng nếu chúng phức tạp)

    static class VocabularyItem {
        int id;
        String word;
        String meaning;
        // int status; // Nếu bạn có cột status
    }

    static class QuestionItem {
        VocabularyItem vocab;
        int type; // 0: fill, 1: choice, 2: pronunciation

        public QuestionItem(VocabularyItem vocab, int type) {
            this.vocab = vocab;
            this.type = type;
        }
    }
}