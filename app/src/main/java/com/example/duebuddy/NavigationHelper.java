package com.example.duebuddy;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    private NavigationHelper() {
    }

    public static void setup(
            Activity activity,
            BottomNavigationView navigationView,
            int selectedItemId,
            int userId) {

        navigationView.setSelectedItemId(selectedItemId);

        updateNotificationBadge(
                navigationView,
                activity,
                userId
        );

        navigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == selectedItemId) {
                return true;
            }

            if (id == R.id.nav_home) {

                open(
                        activity,
                        Home.class
                );

                return true;
            }

            if (id == R.id.nav_bills) {

                open(
                        activity,
                        Bills.class
                );

                return true;
            }

            if (id == R.id.nav_budget) {

                open(
                        activity,
                        BudgetPlanner.class
                );

                return true;
            }

            if (id == R.id.nav_notifications) {

                open(
                        activity,
                        Notifications.class
                );

                return true;
            }

            if (id == R.id.nav_profile) {

                open(
                        activity,
                        Profile.class
                );

                return true;
            }

            return false;
        });
    }

    public static void updateNotificationBadge(
            BottomNavigationView navigationView,
            Activity activity,
            int userId) {

        int unread =
                new DatabaseHelper(activity)
                        .getUnreadNotificationCount(userId);

        navigationView.removeBadge(
                R.id.nav_notifications
        );

        if (unread > 0) {

            navigationView
                    .getOrCreateBadge(
                            R.id.nav_notifications
                    )
                    .setNumber(unread);
        }
    }

    private static void open(
            Activity activity,
            Class<?> destination) {

        Intent intent =
                new Intent(
                        activity,
                        destination
                );

        activity.startActivity(intent);

        activity.finish();
    }
}