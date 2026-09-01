package com.example.duebuddy;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Profile extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private int userId;

    private EditText username;
    private EditText password;
    private EditText phone;
    private BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);
        session = new SessionManager(this);
        userId = session.getUserId();

        username = findViewById(R.id.etProfileUsername);
        password = findViewById(R.id.etProfilePassword);
        phone = findViewById(R.id.etProfilePhone);
        navigationView = findViewById(R.id.bottomNav);

        loadProfile();

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        NavigationHelper.setup(
                this,
                navigationView,
                R.id.nav_profile,
                userId
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        NavigationHelper.updateNotificationBadge(
                navigationView,
                this,
                userId
        );
    }

    private void loadProfile() {
        Cursor cursor = db.getUser(userId);

        try {
            if (cursor.moveToFirst()) {
                username.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("username")
                ));
                password.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("password")
                ));
                phone.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("phone_number")
                ));
            }
        } finally {
            cursor.close();
        }
    }

    private void saveProfile() {
        String newUsername = username.getText().toString().trim();
        String newPassword = password.getText().toString().trim();
        String newPhone = phone.getText().toString().trim();

        if (newUsername.isEmpty() || newPassword.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill all fields.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            boolean updated = db.updateUser(
                    userId,
                    newUsername,
                    newPassword,
                    newPhone
            );

            if (updated) {
                session.login(userId, newUsername);
                Toast.makeText(
                        this,
                        "Profile updated successfully.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Username is already in use.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void logout() {
        session.logout();

        startActivity(new Intent(this, Login.class));
        finishAffinity();
    }
}
