package com.example.vocabulary;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvGoRegister;
    DatabaseHelper dbHelper;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        btnLogin.setOnClickListener(v -> {
            String u = etUsername.getText().toString().trim();
            String p = etPassword.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Kiểm tra tài khoản admin trước
            if (u.equals("admin") && p.equals("admin")) {
                Toast.makeText(this, "Đăng nhập quản trị thành công", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AdminActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // ✅ Kiểm tra tài khoản thường trong bảng User
            Cursor c = db.rawQuery("SELECT * FROM User WHERE username=? AND password=?", new String[]{u, p});
            if (c.moveToFirst()) {
                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", u);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Sai username hoặc password", Toast.LENGTH_SHORT).show();
            }
            c.close();
        });

        tvGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
