package com.example.duebuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Register extends AppCompatActivity {

    EditText username, password, confirmPassword, phoneNumber;
    Button registerButton;

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });

        db = new DatabaseHelper(this);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        phoneNumber = findViewById(R.id.phoneNumber);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {

            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String confirm = confirmPassword.getText().toString().trim();
            String phone = phoneNumber.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty() || phone.isEmpty()) {

                Toast.makeText(Register.this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();

            } else if (!pass.equals(confirm)) {

                Toast.makeText(Register.this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();

            } else {

                boolean success = db.insertUser(user, pass, phone);

                if (success) {

                    Toast.makeText(Register.this,
                            "Registration Successful",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Register.this, Login.class);
                    startActivity(intent);
                    finish();

                } else {

                    Toast.makeText(Register.this,
                            "Username already exists",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}