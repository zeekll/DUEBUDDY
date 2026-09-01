package com.example.duebuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class Bills extends AppCompatActivity {

    private DatabaseHelper db;
    private int userId;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        db = new DatabaseHelper(this);
        userId = new SessionManager(this).getUserId();

        recyclerView = findViewById(R.id.recyclerAllBills);
        emptyMessage = findViewById(R.id.txtNoBillsAll);
        navigationView = findViewById(R.id.bottomNav);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAddBill).setOnClickListener(v -> {
            startActivity(new Intent(this, AddBill.class));
        });

        NavigationHelper.setup(
                this,
                navigationView,
                R.id.nav_bills,
                userId
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBills();
        NavigationHelper.updateNotificationBadge(
                navigationView,
                this,
                userId
        );
    }

    private void loadBills() {
        ArrayList<Bill> bills = db.getBillsList(userId);

        BillAdapter adapter = new BillAdapter(
                bills,
                userId,
                this::loadBills
        );

        recyclerView.setAdapter(adapter);

        boolean hasBills = !bills.isEmpty();
        emptyMessage.setVisibility(hasBills ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hasBills ? View.VISIBLE : View.GONE);
    }
}
