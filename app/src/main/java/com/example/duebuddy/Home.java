package com.example.duebuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class Home extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 200;

    private DatabaseHelper db;
    private SessionManager session;
    private int userId;

    private TextView totalBills;
    private TextView paidBills;
    private TextView unpaidBills;
    private TextView noBills;
    private RecyclerView recyclerView;
    private BottomNavigationView navigationView;

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
        totalBills.setText(String.valueOf(db.countBills(userId)));
        paidBills.setText(String.valueOf(db.countPaidBills(userId)));
        unpaidBills.setText(String.valueOf(db.countUnpaidBills(userId)));

        ArrayList<Bill> bills = db.getBillsList(userId);
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
