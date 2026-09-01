package com.example.duebuddy;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class BudgetPlanner extends AppCompatActivity {

    private TextView tvMonthlyIncome;
    private TextView tvIncomeMonth;
    private TextView tvTotalExpenses;
    private TextView tvRemainingBudget;
    private TextView tvBudgetStatus;
    private TextView txtNoBudgetItems;

    private TextView btnIncomeMenu;

    private Button btnAddBudget;

    private LinearLayout budgetItemsContainer;

    private MaterialCardView monthlyIncomeCard;

    private BottomNavigationView navigationView;

    private DatabaseHelper db;
    private SessionManager session;

    private int userId;

    private double monthlyIncome = 0.0;
    private double totalExpenses = 0.0;

    private String currentMonth;
    private int currentYear;

    private final String[] categories = {
            "Bills",
            "Rent",
            "Savings",
            "Food",
            "Transportation",
            "School",
            "Shopping",
            "Others"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_budgetplanner);

        // =====================================================
        // SESSION
        // =====================================================

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

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        // =====================================================
        // CURRENT MONTH / YEAR
        // =====================================================

        Calendar calendar = Calendar.getInstance();

        currentYear =
                calendar.get(Calendar.YEAR);

        String[] months = {
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

        currentMonth =
                months[calendar.get(Calendar.MONTH)];

        // =====================================================
        // FIND VIEWS
        // =====================================================

        monthlyIncomeCard =
                findViewById(R.id.monthlyIncomeCard);

        tvMonthlyIncome =
                findViewById(R.id.tvMonthlyIncome);

        tvIncomeMonth =
                findViewById(R.id.tvIncomeMonth);

        tvTotalExpenses =
                findViewById(R.id.tvTotalExpenses);

        tvRemainingBudget =
                findViewById(R.id.tvRemainingBudget);

        tvBudgetStatus =
                findViewById(R.id.tvBudgetStatus);

        txtNoBudgetItems =
                findViewById(R.id.txtNoBudgetItems);

        btnIncomeMenu =
                findViewById(R.id.btnIncomeMenu);

        btnAddBudget =
                findViewById(R.id.btnAddBudget);

        budgetItemsContainer =
                findViewById(R.id.budgetItemsContainer);

        navigationView =
                findViewById(R.id.bottomNav);

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        if (navigationView != null) {

            NavigationHelper.setup(
                    this,
                    navigationView,
                    R.id.nav_budget,
                    userId
            );
        }

        // =====================================================
        // MONTHLY INCOME CARD
        // =====================================================

        if (monthlyIncomeCard != null) {

            monthlyIncomeCard.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                BudgetPlanner.this,
                                AddMonthlyIncome.class
                        );

                startActivity(intent);
            });
        }

        // =====================================================
        // MONTHLY INCOME MENU
        // =====================================================

        if (btnIncomeMenu != null) {

            btnIncomeMenu.setOnClickListener(
                    v -> showIncomeMenu()
            );
        }

        // =====================================================
        // LOAD DATA
        // =====================================================

        loadMonthlyIncome();

        loadBudgetItems();

        // =====================================================
        // ADD ALLOCATION
        // =====================================================

        btnAddBudget.setOnClickListener(
                v -> showAddBudgetDialog()
        );
    }

    // =========================================================
    // MONTHLY INCOME MENU
    // =========================================================

    private void showIncomeMenu() {

        PopupMenu popupMenu =
                new PopupMenu(
                        this,
                        btnIncomeMenu
                );

        popupMenu.getMenu().add(
                "Edit Income"
        );

        popupMenu.getMenu().add(
                "Refresh Income"
        );

        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String selected =
                            item.getTitle().toString();

                    if (selected.equals(
                            "Edit Income")) {

                        Intent intent =
                                new Intent(
                                        BudgetPlanner.this,
                                        AddMonthlyIncome.class
                                );

                        startActivity(intent);

                        return true;
                    }

                    if (selected.equals(
                            "Refresh Income")) {

                        loadMonthlyIncome();

                        loadBudgetItems();

                        Toast.makeText(
                                this,
                                "Income and budget refreshed.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    return false;
                }
        );

        popupMenu.show();
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (session == null) {
            return;
        }

        userId =
                session.getUserId();

        if (userId < 0) {
            return;
        }

        loadMonthlyIncome();

        loadBudgetItems();

        if (navigationView != null) {

            NavigationHelper.updateNotificationBadge(
                    navigationView,
                    this,
                    userId
            );
        }
    }

    // =========================================================
    // LOAD MONTHLY INCOME
    // =========================================================

    private void loadMonthlyIncome() {

        monthlyIncome =
                db.getCurrentMonthlyIncome(
                        userId,
                        currentMonth,
                        currentYear
                );

        tvMonthlyIncome.setText(
                formatPeso(monthlyIncome)
        );

        tvIncomeMonth.setText(
                currentMonth + " " + currentYear
        );

        updateSummary();
    }

    // =========================================================
    // LOAD BUDGET ITEMS
    // =========================================================

    private void loadBudgetItems() {

        budgetItemsContainer.removeAllViews();

        totalExpenses = 0.0;

        android.database.Cursor cursor =
                db.getBudgetItems(userId);

        if (cursor == null) {

            if (txtNoBudgetItems != null) {

                txtNoBudgetItems.setVisibility(
                        View.VISIBLE
                );
            }

            updateSummary();

            return;
        }

        boolean hasItems = false;

        try {

            while (cursor.moveToNext()) {

                hasItems = true;

                String category =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "category"
                                )
                        );

                String description =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "description"
                                )
                        );

                double amount =
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "amount"
                                )
                        );

                totalExpenses += amount;

                addBudgetItem(
                        category,
                        description,
                        amount
                );
            }

        } finally {

            cursor.close();
        }

        if (txtNoBudgetItems != null) {

            txtNoBudgetItems.setVisibility(
                    hasItems
                            ? View.GONE
                            : View.VISIBLE
            );
        }

        updateSummary();
    }

    // =========================================================
    // ADD BUDGET DIALOG
    // =========================================================

    private void showAddBudgetDialog() {

        Dialog dialog =
                new Dialog(this);

        dialog.setContentView(
                R.layout.dialog_addbudget
        );

        Spinner spinnerBudgetCategory =
                dialog.findViewById(
                        R.id.spinnerBudgetCategory
                );

        EditText etBudgetDescription =
                dialog.findViewById(
                        R.id.etBudgetDescription
                );

        EditText etBudgetAmount =
                dialog.findViewById(
                        R.id.etBudgetAmount
                );

        Button btnSaveBudgetItem =
                dialog.findViewById(
                        R.id.btnSaveBudgetItem
                );

        // =====================================================
        // CATEGORY
        // =====================================================

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerBudgetCategory.setAdapter(adapter);

        // =====================================================
        // SAVE
        // =====================================================

        btnSaveBudgetItem.setOnClickListener(v -> {

            String category =
                    spinnerBudgetCategory
                            .getSelectedItem()
                            .toString();

            String description =
                    etBudgetDescription
                            .getText()
                            .toString()
                            .trim();

            String amountText =
                    etBudgetAmount
                            .getText()
                            .toString()
                            .trim();

            if (description.isEmpty()) {

                etBudgetDescription.setError(
                        "Enter a description"
                );

                return;
            }

            if (amountText.isEmpty()) {

                etBudgetAmount.setError(
                        "Enter an amount"
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

                etBudgetAmount.setError(
                        "Enter a valid amount"
                );

                return;
            }

            if (amount <= 0) {

                etBudgetAmount.setError(
                        "Amount must be greater than zero"
                );

                return;
            }

            long result =
                    db.insertBudgetItem(
                            userId,
                            category,
                            description,
                            amount
                    );

            if (result == -1) {

                Toast.makeText(
                        this,
                        "Failed to save allocation.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Toast.makeText(
                    this,
                    "Allocation saved!",
                    Toast.LENGTH_SHORT
            ).show();

            dialog.dismiss();

            loadBudgetItems();
        });

        dialog.show();
    }

    // =========================================================
    // DISPLAY BUDGET ITEM
    // =========================================================

    private void addBudgetItem(
            String category,
            String description,
            double amount) {

        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                10
        );

        card.setLayoutParams(cardParams);

        card.setRadius(16);

        card.setCardElevation(1);

        card.setCardBackgroundColor(
                Color.WHITE
        );

        card.setStrokeColor(
                Color.rgb(225, 225, 225)
        );

        card.setStrokeWidth(1);

        // =====================================================
        // CONTENT
        // =====================================================

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.HORIZONTAL
        );

        content.setGravity(
                Gravity.CENTER_VERTICAL
        );

        content.setPadding(
                15,
                13,
                15,
                13
        );

        // =====================================================
        // ICON
        // =====================================================

        TextView icon =
                new TextView(this);

        icon.setLayoutParams(
                new LinearLayout.LayoutParams(
                        42,
                        42
                )
        );

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setText(
                getCategoryIcon(category)
        );

        icon.setTextSize(18);

        icon.setTextColor(
                Color.rgb(50, 150, 80)
        );

        // =====================================================
        // TEXT AREA
        // =====================================================

        LinearLayout textContainer =
                new LinearLayout(this);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        textParams.setMargins(
                12,
                0,
                8,
                0
        );

        textContainer.setLayoutParams(
                textParams
        );

        textContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView categoryText =
                new TextView(this);

        categoryText.setText(
                category
        );

        categoryText.setTextColor(
                Color.rgb(45, 45, 45)
        );

        categoryText.setTextSize(15);

        categoryText.setTypeface(
                null,
                Typeface.BOLD
        );

        TextView descriptionText =
                new TextView(this);

        descriptionText.setText(
                description
        );

        descriptionText.setTextColor(
                Color.rgb(120, 120, 120)
        );

        descriptionText.setTextSize(12);

        descriptionText.setMaxLines(2);

        textContainer.addView(
                categoryText
        );

        textContainer.addView(
                descriptionText
        );

        // =====================================================
        // AMOUNT
        // =====================================================

        TextView amountText =
                new TextView(this);

        amountText.setText(
                formatPeso(amount)
        );

        amountText.setTextColor(
                Color.rgb(46, 145, 70)
        );

        amountText.setTextSize(15);

        amountText.setTypeface(
                null,
                Typeface.BOLD
        );

        // =====================================================
        // ADD VIEWS
        // =====================================================

        content.addView(icon);

        content.addView(
                textContainer
        );

        content.addView(
                amountText
        );

        card.addView(content);

        budgetItemsContainer.addView(card);
    }

    // =========================================================
    // UPDATE SUMMARY
    // =========================================================

    private void updateSummary() {

        double remaining =
                monthlyIncome - totalExpenses;

        tvTotalExpenses.setText(
                formatPeso(totalExpenses)
        );

        tvRemainingBudget.setText(
                formatPeso(remaining)
        );

        if (monthlyIncome <= 0) {

            tvBudgetStatus.setText(
                    "No Income Set"
            );

            tvBudgetStatus.setTextColor(
                    Color.rgb(120, 120, 120)
            );

        } else if (remaining >= 0) {

            tvBudgetStatus.setText(
                    "Within Budget"
            );

            tvBudgetStatus.setTextColor(
                    Color.rgb(46, 155, 75)
            );

        } else {

            tvBudgetStatus.setText(
                    "Over Budget"
            );

            tvBudgetStatus.setTextColor(
                    Color.rgb(200, 60, 60)
            );
        }
    }

    // =========================================================
    // CATEGORY ICON
    // =========================================================

    private String getCategoryIcon(
            String category) {

        if (category == null) {
            return "•";
        }

        switch (category) {

            case "Bills":
                return "▣";

            case "Rent":
                return "⌂";

            case "Savings":
                return "₱";

            case "Food":
                return "🍴";

            case "Transportation":
                return "▣";

            case "School":
                return "▤";

            case "Shopping":
                return "□";

            default:
                return "•";
        }
    }

    // =========================================================
    // PESO FORMAT
    // =========================================================

    private String formatPeso(
            double amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.US
                );

        formatter.setMinimumFractionDigits(2);

        formatter.setMaximumFractionDigits(2);

        return "₱" +
                formatter.format(amount);
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        Intent intent =
                new Intent(
                        this,
                        Home.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        finish();
    }
}