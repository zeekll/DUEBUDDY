package com.example.duebuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddMonthlyIncome extends AppCompatActivity {

    private EditText etIncomeAmount;

    private Spinner spinnerSource;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;

    private Button btnSaveIncome;

    private TextView tvCurrentIncome;
    private TextView tvCurrentIncomeMonth;
    private TextView tvEditIncome;

    private DatabaseHelper db;
    private SessionManager session;

    private int userId;

    private final String[] months = {
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
    };

    private final String[] sources = {
            "Salary",
            "Allowance",
            "Business",
            "Freelance",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_add_monthly_income
        );

        session = new SessionManager(this);
        userId = session.getUserId();

        if (userId < 0) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            startActivity(
                    new Intent(this, Login.class)
            );

            finish();
            return;
        }

        db = new DatabaseHelper(this);

        ImageButton btnBack =
                findViewById(R.id.btnBack);

        etIncomeAmount =
                findViewById(R.id.etIncomeAmount);

        spinnerSource =
                findViewById(R.id.spinnerIncomeSource);

        spinnerMonth =
                findViewById(R.id.spinnerIncomeMonth);

        spinnerYear =
                findViewById(R.id.spinnerIncomeYear);

        btnSaveIncome =
                findViewById(R.id.btnSaveIncome);

        tvCurrentIncome =
                findViewById(R.id.tvCurrentIncome);

        tvCurrentIncomeMonth =
                findViewById(R.id.tvCurrentIncomeMonth);

        tvEditIncome =
                findViewById(R.id.tvEditIncome);

        btnBack.setOnClickListener(
                v -> finish()
        );

        setupSourceSpinner();
        setupMonthSpinner();
        setupYearSpinner();

        setCurrentMonthAndYear();

        loadCurrentIncome();

        btnSaveIncome.setOnClickListener(
                v -> saveIncome()
        );

        tvEditIncome.setOnClickListener(v -> {

            etIncomeAmount.requestFocus();

            etIncomeAmount.setSelection(
                    etIncomeAmount
                            .getText()
                            .length()
            );
        });

        spinnerMonth.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        loadCurrentIncome();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );

        spinnerYear.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        loadCurrentIncome();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    private void setupSourceSpinner() {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sources
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSource.setAdapter(adapter);
    }

    private void setupMonthSpinner() {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        months
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMonth.setAdapter(adapter);
    }

    private void setupYearSpinner() {

        int currentYear =
                Calendar.getInstance()
                        .get(Calendar.YEAR);

        String[] years = new String[6];

        for (int i = 0; i < years.length; i++) {

            years[i] =
                    String.valueOf(
                            currentYear - i
                    );
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        years
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerYear.setAdapter(adapter);
    }

    private void setCurrentMonthAndYear() {

        Calendar calendar =
                Calendar.getInstance();

        int currentMonth =
                calendar.get(
                        Calendar.MONTH
                );

        spinnerMonth.setSelection(
                currentMonth
        );

        int currentYear =
                calendar.get(
                        Calendar.YEAR
                );

        ArrayAdapter adapter =
                (ArrayAdapter)
                        spinnerYear.getAdapter();

        int yearPosition =
                adapter.getPosition(
                        String.valueOf(currentYear)
                );

        if (yearPosition >= 0) {

            spinnerYear.setSelection(
                    yearPosition
            );
        }
    }

    private void saveIncome() {

        String amountText =
                etIncomeAmount
                        .getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            etIncomeAmount.setError(
                    "Enter your income amount"
            );

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountText
                    );

        } catch (NumberFormatException e) {

            etIncomeAmount.setError(
                    "Enter a valid amount"
            );

            return;
        }

        if (amount <= 0) {

            etIncomeAmount.setError(
                    "Amount must be greater than zero"
            );

            return;
        }

        String source =
                spinnerSource
                        .getSelectedItem()
                        .toString();

        String month =
                spinnerMonth
                        .getSelectedItem()
                        .toString();

        int year =
                Integer.parseInt(
                        spinnerYear
                                .getSelectedItem()
                                .toString()
                );

        boolean saved =
                db.saveMonthlyIncome(
                        userId,
                        amount,
                        source,
                        month,
                        year
                );

        if (!saved) {

            Toast.makeText(
                    this,
                    "Failed to save income.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Income saved successfully!",
                Toast.LENGTH_SHORT
        ).show();

        // Go directly to Budget Planner
        Intent intent =
                new Intent(
                        this,
                        BudgetPlanner.class
                );

        intent.putExtra(
                "selected_month",
                month
        );

        intent.putExtra(
                "selected_year",
                year
        );

        startActivity(intent);

        finish();
    }

    private void loadCurrentIncome() {

        if (spinnerMonth.getSelectedItem() == null ||
                spinnerYear.getSelectedItem() == null) {

            return;
        }

        String month =
                spinnerMonth
                        .getSelectedItem()
                        .toString();

        int year =
                Integer.parseInt(
                        spinnerYear
                                .getSelectedItem()
                                .toString()
                );

        double income =
                db.getCurrentMonthlyIncome(
                        userId,
                        month,
                        year
                );

        String source =
                db.getMonthlyIncomeSource(
                        userId,
                        month,
                        year
                );

        if (income > 0) {

            etIncomeAmount.setText(
                    String.format(
                            "%.2f",
                            income
                    )
            );

            if (!source.isEmpty()) {

                int sourcePosition =
                        ((ArrayAdapter<String>)
                                spinnerSource.getAdapter())
                                .getPosition(source);

                if (sourcePosition >= 0) {

                    spinnerSource.setSelection(
                            sourcePosition
                    );
                }
            }

            tvCurrentIncome.setText(
                    String.format(
                            "₱%,.2f",
                            income
                    )
            );

        } else {

            etIncomeAmount.setText("");

            tvCurrentIncome.setText(
                    "₱0.00"
            );
        }

        tvCurrentIncomeMonth.setText(
                month + " " + year
        );

        tvEditIncome.setVisibility(
                TextView.VISIBLE
        );
    }
}