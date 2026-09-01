package com.example.duebuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class Notifications extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private TextView txtNoNotifications;
    private Button btnMarkAllRead;
    private BottomNavigationView navigationView;

    private DatabaseHelper db;
    private SessionManager session;
    private NotificationAdapter adapter;
    private ArrayList<AppNotification> notificationList;

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        session = new SessionManager(this);
        userId = session.getUserId();

        if (userId < 0) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        db = new DatabaseHelper(this);

        // Find views
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        txtNoNotifications = findViewById(R.id.txtNoNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        navigationView = findViewById(R.id.bottomNav);

        // Set up bottom navigation
        NavigationHelper.setup(this, navigationView, R.id.nav_notifications, userId);

        // Set up RecyclerView
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        loadNotifications();

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session != null) {
            userId = session.getUserId();
            if (userId >= 0) {
                loadNotifications();
                NavigationHelper.updateNotificationBadge(navigationView, this, userId);
            }
        }
    }

    private void loadNotifications() {
        notificationList = db.getNotifications(userId);

        if (notificationList == null || notificationList.isEmpty()) {
            txtNoNotifications.setVisibility(View.VISIBLE);
            recyclerNotifications.setVisibility(View.GONE);
            btnMarkAllRead.setVisibility(View.GONE);
        } else {
            txtNoNotifications.setVisibility(View.GONE);
            recyclerNotifications.setVisibility(View.VISIBLE);
            btnMarkAllRead.setVisibility(View.VISIBLE);

            adapter = new NotificationAdapter(
                    notificationList,
                    notification -> {
                        // Mark as read when clicked
                        db.markNotificationRead(notification.getId(), userId);
                        
                        // Navigate to Bill details if needed
                        Intent intent = new Intent(this, Bills.class);
                        intent.putExtra("BILL_ID", notification.getBillId());
                        startActivity(intent);
                        
                        loadNotifications();
                    },
                    notification -> {
                        // Delete notification
                        if (db.deleteNotification(notification.getId(), userId)) {
                            loadNotifications();
                            Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            recyclerNotifications.setAdapter(adapter);
        }
    }

    private void markAllAsRead() {
        db.markAllNotificationsRead(userId);
        loadNotifications();
        NavigationHelper.updateNotificationBadge(navigationView, this, userId);
        Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, Home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
