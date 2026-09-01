package com.example.duebuddy;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Calendar;

public class AddBill extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;
    private static final int SMS_PERMISSION_REQUEST = 101;

    private EditText billName;
    private EditText accountHolder;
    private EditText amount;
    private EditText dueDate;
    private EditText startDate;
    private EditText endDate;
    private EditText notes;
    private Spinner category;
    private CheckBox recurring;
    private Button saveButton;

    private DatabaseHelper db;
    private int userId;
    private int billId = -1;

    private final String[] categories = {
            "Electricity",
            "Water",
            "Internet",
            "Phone",
            "Rent",
            "Tuition",
            "Loan",
            "Subscription",
            "Insurance",
            "Others"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addbill);

        db = new DatabaseHelper(this);
        userId = new SessionManager(this).getUserId();
        billId = getIntent().getIntExtra("bill_id", -1);

        bindViews();
        setupCategorySpinner();
        setupDatePickers();

        if (billId != -1) {
            loadBillForEdit();
        }

        saveButton.setText(
                billId == -1 ? "SAVE BILL" : "UPDATE BILL"
        );

        saveButton.setOnClickListener(v -> saveBill());

        NotificationHelper.createChannel(this);
        requestNotificationPermission();
        requestSmsPermission();
    }

    private void bindViews() {
        billName = findViewById(R.id.etBillName);
        accountHolder = findViewById(R.id.etAccountHolder);
        amount = findViewById(R.id.etAmount);
        dueDate = findViewById(R.id.etDueDate);
        startDate = findViewById(R.id.etStartDate);
        endDate = findViewById(R.id.etEndDate);
        notes = findViewById(R.id.etNotes);
        category = findViewById(R.id.spCategory);
        recurring = findViewById(R.id.cbRecurring);
        saveButton = findViewById(R.id.btnSave);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );

        category.setAdapter(adapter);
    }

    private void setupDatePickers() {
        dueDate.setOnClickListener(v -> showDatePicker(dueDate));
        startDate.setOnClickListener(v -> showDatePicker(startDate));
        endDate.setOnClickListener(v -> showDatePicker(endDate));
    }

    private void loadBillForEdit() {
        Bill bill = db.getBill(billId, userId);

        if (bill == null) {
            return;
        }

        billName.setText(bill.getBillName());
        accountHolder.setText(bill.getAccountHolder());
        amount.setText(String.valueOf(bill.getAmount()));
        dueDate.setText(bill.getDueDate());
        recurring.setChecked(bill.getRecurring() == 1);
        startDate.setText(bill.getStartDate());
        endDate.setText(bill.getEndDate());
        notes.setText(bill.getNotes());

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(bill.getCategory())) {
                category.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> target.setText(
                        year + "-" + (month + 1) + "-" + day
                ),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void saveBill() {
        String name = billName.getText().toString().trim();
        String holder = accountHolder.getText().toString().trim();
        String amountText = amount.getText().toString().trim();
        String due = dueDate.getText().toString().trim();
        String start = startDate.getText().toString().trim();
        String end = endDate.getText().toString().trim();
        String noteText = notes.getText().toString().trim();
        String selectedCategory = category.getSelectedItem().toString();

        if (name.isEmpty() || holder.isEmpty() || amountText.isEmpty() || due.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill in all required fields.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        double billAmount;

        try {
            billAmount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Please enter a valid amount.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int recurringValue = recurring.isChecked() ? 1 : 0;
        boolean success;
        int savedBillId = billId;

        if (billId == -1) {
            savedBillId = db.insertBillAndGetId(
                    userId,
                    name,
                    holder,
                    selectedCategory,
                    billAmount,
                    due,
                    recurringValue,
                    start,
                    end,
                    noteText
            );

            success = savedBillId != -1;
        } else {
            success = db.updateBill(
                    billId,
                    userId,
                    name,
                    holder,
                    selectedCategory,
                    billAmount,
                    due,
                    recurringValue,
                    start,
                    end,
                    noteText
            );
        }

        if (!success) {
            Toast.makeText(
                    this,
                    "Unable to save bill.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Bill savedBill = db.getBill(savedBillId, userId);

        if (savedBill != null) {
            ReminderScheduler.schedule(this, savedBill);
        }

        Toast.makeText(
                this,
                billId == -1
                        ? "Bill saved successfully!"
                        : "Bill updated successfully!",
                Toast.LENGTH_SHORT
        ).show();

        startActivity(new Intent(this, Home.class));
        finish();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private void requestSmsPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST
            );
        }
    }
}
