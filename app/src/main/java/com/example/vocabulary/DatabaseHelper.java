package com.example.vocabulary;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "vocabulary";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng
        db.execSQL("CREATE TABLE Course (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "word_count INTEGER, " +
                "progress INTEGER)");

        db.execSQL("CREATE TABLE Topic (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_id INTEGER, " +
                "name TEXT, " +
                "word_count INTEGER)");

        db.execSQL("CREATE TABLE Vocabulary (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "topic_id INTEGER, " +
                "word TEXT, " +
                "definition TEXT, " +
                "example TEXT, " +
                "meaning_vi TEXT, " +
                "sound TEXT, " +
                "status INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE Detail_Vocabulary (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vocab_id INTEGER, " +
                "synonyms TEXT, " +
                "FOREIGN KEY(vocab_id) REFERENCES Vocabulary(id))");

        db.execSQL("CREATE TABLE User (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT, " +
                "email TEXT, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        // Thêm khóa học
        String[] courseNames = {"TOEIC", "TOEFL", "SAT", "IELTS", "Business English", "Basic English"};
        ContentValues course = new ContentValues();
        for (String courseName : courseNames) {
            course.clear();
            course.put("name", courseName);
            course.put("word_count", 0);
            course.put("progress", 0);
            db.insert("Course", null, course);
        }

        // Thêm 10 chủ đề TOEIC
        String[] topics = {
                "Office", "Meetings", "Travel", "Marketing", "Finance",
                "Contracts", "Computers", "Customer Service", "Employment", "Manufacturing"
        };
        ContentValues topicValues = new ContentValues();
        for (String topic : topics) {
            topicValues.clear();
            topicValues.put("course_id", 1);
            topicValues.put("name", topic);
            topicValues.put("word_count", 0);
            db.insert("Topic", null, topicValues);
        }

        // Dữ liệu từ vựng (50 từ - 5 chủ đề đầu)
        String[][] words = {
                {"stationery", "materials for writing", "She bought new stationery.", "đồ dùng văn phòng", "stationery.mp3"},
                {"photocopier", "a machine that makes copies", "The photocopier is broken.", "máy photocopy", "photocopier.mp3"},
                {"printer", "a device for printing", "We need a new printer.", "máy in", "printer.mp3"},
                {"desk", "a piece of furniture", "He sat at his desk.", "bàn làm việc", "desk.mp3"},
                {"memo", "a written reminder", "I sent her a memo.", "bản ghi nhớ", "memo.mp3"},
                {"cubicle", "a small office space", "He works in a cubicle.", "gian làm việc", "cubicle.mp3"},
                {"filing cabinet", "a cabinet to store files", "Put it in the filing cabinet.", "tủ đựng tài liệu", "filing_cabinet.mp3"},
                {"whiteboard", "a board to write on", "He wrote on the whiteboard.", "bảng trắng", "whiteboard.mp3"},
                {"conference room", "meeting room", "We met in the conference room.", "phòng họp", "conference_room.mp3"},
                {"supplies", "necessary items", "Office supplies are low.", "vật dụng", "supplies.mp3"},
                // Meetings
                {"agenda", "a list of topics", "Check the meeting agenda.", "chương trình họp", "agenda.mp3"},
                {"minutes", "record of a meeting", "She wrote the minutes.", "biên bản họp", "minutes.mp3"},
                {"chairperson", "meeting leader", "The chairperson started the meeting.", "chủ tọa", "chairperson.mp3"},
                {"participant", "attendee", "Participants signed in.", "người tham dự", "participant.mp3"},
                {"schedule", "plan for events", "The meeting is scheduled.", "lịch trình", "schedule.mp3"},
                {"proposal", "a plan or suggestion", "I made a proposal.", "đề xuất", "proposal.mp3"},
                {"vote", "to choose formally", "They voted on the issue.", "bỏ phiếu", "vote.mp3"},
                {"motion", "a formal suggestion", "He made a motion.", "kiến nghị", "motion.mp3"},
                {"consensus", "general agreement", "We reached a consensus.", "đồng thuận", "consensus.mp3"},
                {"adjourn", "end a meeting", "The meeting was adjourned.", "kết thúc", "adjourn.mp3"},
                // Travel
                {"itinerary", "a travel plan", "Here is your itinerary.", "lịch trình du lịch", "itinerary.mp3"},
                {"boarding pass", "a ticket to board", "Show your boarding pass.", "thẻ lên máy bay", "boarding_pass.mp3"},
                {"luggage", "bags for travel", "Check your luggage.", "hành lý", "luggage.mp3"},
                {"passport", "travel document", "Show your passport.", "hộ chiếu", "passport.mp3"},
                {"visa", "entry permit", "He needs a visa.", "thị thực", "visa.mp3"},
                {"departure", "leaving", "The departure is delayed.", "khởi hành", "departure.mp3"},
                {"arrival", "coming", "The arrival time is 6 PM.", "đến nơi", "arrival.mp3"},
                {"reservation", "a booking", "I have a reservation.", "đặt chỗ", "reservation.mp3"},
                {"customs", "border control", "Go through customs.", "hải quan", "customs.mp3"},
                {"souvenir", "travel gift", "I bought a souvenir.", "quà lưu niệm", "souvenir.mp3"},
                // Marketing
                {"advertisement", "a public promotion", "I saw the advertisement.", "quảng cáo", "advertisement.mp3"},
                {"brand", "a product name", "That’s a famous brand.", "thương hiệu", "brand.mp3"},
                {"slogan", "a catchphrase", "The slogan is catchy.", "khẩu hiệu", "slogan.mp3"},
                {"target audience", "intended viewers", "Define your target audience.", "khách hàng mục tiêu", "target_audience.mp3"},
                {"promotion", "marketing strategy", "We have a new promotion.", "khuyến mãi", "promotion.mp3"},
                {"market research", "study of market", "They did market research.", "nghiên cứu thị trường", "market_research.mp3"},
                {"commercial", "TV/radio ad", "I watched a commercial.", "quảng cáo", "commercial.mp3"},
                {"campaign", "organized effort", "Launch a new campaign.", "chiến dịch", "campaign.mp3"},
                {"strategy", "plan of action", "Use a better strategy.", "chiến lược", "strategy.mp3"},
                {"consumer", "a buyer", "Know your consumer.", "người tiêu dùng", "consumer.mp3"},
                // Finance
                {"budget", "financial plan", "We set a budget.", "ngân sách", "budget.mp3"},
                {"invoice", "bill", "Send the invoice.", "hóa đơn", "invoice.mp3"},
                {"expense", "cost", "Cut down expenses.", "chi phí", "expense.mp3"},
                {"revenue", "income", "Revenue is growing.", "doanh thu", "revenue.mp3"},
                {"profit", "money gained", "Profit increased.", "lợi nhuận", "profit.mp3"},
                {"loss", "money lost", "The company had a loss.", "thua lỗ", "loss.mp3"},
                {"salary", "monthly pay", "Her salary increased.", "lương", "salary.mp3"},
                {"accounting", "financial record-keeping", "He studies accounting.", "kế toán", "accounting.mp3"},
                {"audit", "official inspection", "They did an audit.", "kiểm toán", "audit.mp3"},
                {"balance sheet", "financial summary", "Check the balance sheet.", "bảng cân đối kế toán", "balance_sheet.mp3"},
        };

        String[] synonymsList = {
                "writing materials, office supplies",
                "copy machine, duplicator",
                "printing device",
                "worktable, workspace",
                "note, reminder",
                "cubicle office, booth",
                "file cabinet, document storage",
                "dry-erase board",
                "meeting room, boardroom",
                "equipment, resources",
                "schedule, plan",
                "record, log",
                "moderator, head",
                "attendee, member",
                "timetable, calendar",
                "suggestion, offer",
                "ballot, decision",
                "proposal, suggestion",
                "agreement, unity",
                "close, end",
                "travel plan, schedule",
                "boarding ticket",
                "baggage, bags",
                "travel ID",
                "travel permit",
                "takeoff, leaving",
                "arrival time, landing",
                "booking, appointment",
                "immigration, border check",
                "gift, keepsake"
        };

        // Thêm từ vựng và đồng nghĩa
        ContentValues wordValues = new ContentValues();
        ContentValues detailValues = new ContentValues();
        int wordIndex = 0;
        for (int topicId = 1; topicId <= 5; topicId++) {
            for (int i = 0; i < 10; i++) {
                String[] w = words[wordIndex];
                wordValues.clear();
                wordValues.put("topic_id", topicId);
                wordValues.put("word", w[0]);
                wordValues.put("definition", w[1]);
                wordValues.put("example", w[2]);
                wordValues.put("meaning_vi", w[3]);
                wordValues.put("sound", w[4]);

                long vocabId = db.insert("Vocabulary", null, wordValues);

                if (wordIndex < 30) {
                    detailValues.clear();
                    detailValues.put("vocab_id", vocabId);
                    detailValues.put("synonyms", synonymsList[wordIndex]);
                    db.insert("Detail_Vocabulary", null, detailValues);
                }

                wordIndex++;
            }
        }

        db.execSQL("UPDATE Topic SET word_count = (SELECT COUNT(*) FROM Vocabulary WHERE Vocabulary.topic_id = Topic.id)");
        db.execSQL("UPDATE Course SET word_count = (SELECT SUM(word_count) FROM Topic WHERE Topic.course_id = Course.id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Detail_Vocabulary");
        db.execSQL("DROP TABLE IF EXISTS Vocabulary");
        db.execSQL("DROP TABLE IF EXISTS Topic");
        db.execSQL("DROP TABLE IF EXISTS Course");
        db.execSQL("DROP TABLE IF EXISTS User");
        onCreate(db);
    }
}
