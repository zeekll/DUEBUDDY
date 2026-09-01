package com.example.duebuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 200;

    // Dashboard pie chart colors: Paid / Unpaid / Due Soon / Overdue.
    private static final int COLOR_PAID = Color.parseColor("#4CAF50");
    private static final int COLOR_UNPAID = Color.parseColor("#42A5F5");
    private static final int COLOR_DUE_SOON = Color.parseColor("#FFA726");
    private static final int COLOR_OVERDUE = Color.parseColor("#EF5350");

    private DatabaseHelper db;
    private SessionManager session;
    private int userId;

    private TextView totalBills;
    private TextView paidBills;
    private TextView unpaidBills;
    private TextView noBills;
    private RecyclerView recyclerView;
    private BottomNavigationView navigationView;

    private PieChartView pieChartBills;
    private TextView txtChartTotal;
    private TextView txtLegendPaid;
    private TextView txtLegendUnpaid;
    private TextView txtLegendDueSoon;
    private TextView txtLegendOverdue;
    private TextView txtPaidThisMonth;
    private TextView txtUnpaidBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);
        userId = session.getUserId();

        if (userId < 0) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        db = new DatabaseHelper(this);

        bindViews();
        setupRecyclerView();
        setupActions();

        NotificationHelper.createChannel(this);
        requestNotificationPermission();
        rescheduleUserBills();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            return;
        }

        userId = session.getUserId();
        loadDashboard();

        if (navigationView != null) {
            NavigationHelper.updateNotificationBadge(
                    navigationView,
                    this,
                    userId
            );
        }
    }

    private void bindViews() {
        totalBills = findViewById(R.id.txtTotalBills);
        paidBills = findViewById(R.id.txtPaidBills);
        unpaidBills = findViewById(R.id.txtUnpaidBills);
        noBills = findViewById(R.id.txtNoBills);
        recyclerView = findViewById(R.id.recyclerBills);
        navigationView = findViewById(R.id.bottomNav);

        pieChartBills = findViewById(R.id.pieChartBills);
        txtChartTotal = findViewById(R.id.txtChartTotal);
        txtLegendPaid = findViewById(R.id.txtLegendPaid);
        txtLegendUnpaid = findViewById(R.id.txtLegendUnpaid);
        txtLegendDueSoon = findViewById(R.id.txtLegendDueSoon);
        txtLegendOverdue = findViewById(R.id.txtLegendOverdue);
        txtPaidThisMonth = findViewById(R.id.txtPaidThisMonth);
        txtUnpaidBalance = findViewById(R.id.txtUnpaidBalance);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupActions() {
        findViewById(R.id.btnAddBill).setOnClickListener(v -> {
            startActivity(new Intent(this, AddBill.class));
        });

        NavigationHelper.setup(
                this,
                navigationView,
                R.id.nav_home,
                userId
        );
    }

    private void loadDashboard() {
        ArrayList<Bill> bills = db.getBillsList(userId);

        updateBillsOverview(bills);

        ArrayList<Bill> recentBills = new ArrayList<>(
                bills.subList(0, Math.min(3, bills.size()))
        );

        BillAdapter adapter = new BillAdapter(
                recentBills,
                userId,
                this::loadDashboard
        );

        recyclerView.setAdapter(adapter);

        boolean hasBills = !recentBills.isEmpty();
        noBills.setVisibility(hasBills ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hasBills ? View.VISIBLE : View.GONE);
    }

    /**
     * Buckets every bill into Paid / Unpaid / Due Soon / Overdue,
     * then drives the top stat row, the pie chart + legend, and the
     * "paid this month" / "remaining unpaid balance" totals.
     */
    private void updateBillsOverview(ArrayList<Bill> bills) {
        int paidCount = 0;
        int unpaidCount = 0;
        int dueSoonCount = 0;
        int overdueCount = 0;

        double paidThisMonth = 0.0;
        double unpaidBalance = 0.0;

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH);

        for (Bill bill : bills) {
            String category = bill.getDashboardCategory();

            if (Bill.CATEGORY_PAID.equals(category)) {
                paidCount++;

                if (bill.isDueInMonth(currentYear, currentMonth)) {
                    paidThisMonth += bill.getAmount();
                }
            } else if (Bill.CATEGORY_OVERDUE.equals(category)) {
                overdueCount++;
                unpaidBalance += bill.getAmount();
            } else if (Bill.CATEGORY_DUE_SOON.equals(category)) {
                dueSoonCount++;
                unpaidBalance += bill.getAmount();
            } else {
                unpaidCount++;
                unpaidBalance += bill.getAmount();
            }
        }

        int totalCount = bills.size();
        int notPaidCount = unpaidCount + dueSoonCount + overdueCount;

        totalBills.setText(String.valueOf(totalCount));
        paidBills.setText(String.valueOf(paidCount));
        unpaidBills.setText(String.valueOf(notPaidCount));

        if (txtChartTotal != null) {
            txtChartTotal.setText(String.valueOf(totalCount));
        }

        if (txtLegendPaid != null) {
            txtLegendPaid.setText(String.valueOf(paidCount));
        }

        if (txtLegendUnpaid != null) {
            txtLegendUnpaid.setText(String.valueOf(unpaidCount));
        }

        if (txtLegendDueSoon != null) {
            txtLegendDueSoon.setText(String.valueOf(dueSoonCount));
        }

        if (txtLegendOverdue != null) {
            txtLegendOverdue.setText(String.valueOf(overdueCount));
        }

        if (txtPaidThisMonth != null) {
            txtPaidThisMonth.setText(formatPeso(paidThisMonth));
        }

        if (txtUnpaidBalance != null) {
            txtUnpaidBalance.setText(formatPeso(unpaidBalance));
        }

        if (pieChartBills != null) {
            List<PieChartView.Slice> slices = new ArrayList<>();
            slices.add(new PieChartView.Slice("Paid", paidCount, COLOR_PAID));
            slices.add(new PieChartView.Slice("Unpaid", unpaidCount, COLOR_UNPAID));
            slices.add(new PieChartView.Slice("Due Soon", dueSoonCount, COLOR_DUE_SOON));
            slices.add(new PieChartView.Slice("Overdue", overdueCount, COLOR_OVERDUE));
            pieChartBills.setSlices(slices);
        }
    }

    private String formatPeso(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "\u20B1" + formatter.format(amount);
    }

    private void rescheduleUserBills() {
        ArrayList<Bill> bills = db.getBillsList(userId);

        for (Bill bill : bills) {
            ReminderScheduler.schedule(this, bill);
        }
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
}