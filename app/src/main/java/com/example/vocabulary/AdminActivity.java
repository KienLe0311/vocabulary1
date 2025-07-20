package com.example.vocabulary;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    SQLiteDatabase db;
    ListView listViewUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        listViewUsers = findViewById(R.id.listViewUsers);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        ArrayList<String> userList = new ArrayList<>();

        // Lấy đủ thông tin từ bảng User
        Cursor c = db.rawQuery("SELECT id, username, password, email, created_at FROM User", null);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String username = c.getString(1);
            String password = c.getString(2);
            String email = c.getString(3);
            String createdAt = c.getString(4);

            String info = "ID: " + id +
                    "\nTài Khoản: " + username +
                    "\nMật Khẩu: " + password +
                    "\nEmail: " + email +
                    "\nThời Gian Tạo: " + createdAt;
            userList.add(info);
        }
        c.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                userList
        );
        listViewUsers.setAdapter(adapter);
    }
}
