package com.example.duebuddy;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button loginButton;
    private Button registerButton;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.btnLogin);
        registerButton = findViewById(R.id.btnRegister);

        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(this, Register.class));
        });

        loginButton.setOnClickListener(v -> login());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void login() {
        String enteredUsername = username.getText().toString().trim();
        String enteredPassword = password.getText().toString().trim();

        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter username and password",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Cursor cursor = db.getUserByCredentials(
                enteredUsername,
                enteredPassword
        );

        try {
            if (cursor.moveToFirst()) {
                int userId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                new SessionManager(this).login(
                        userId,
                        enteredUsername
                );

                Toast.makeText(
                        this,
                        "Login Successful",
                        Toast.LENGTH_SHORT
                ).show();

                startActivity(new Intent(this, Home.class));
                finish();
            } else {
                Toast.makeText(
                        this,
                        "Invalid Username or Password",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } finally {
            cursor.close();
        }
    }
}
