package com.example.duebuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID = "bill_reminders_v2";
    private static final String CHANNEL_NAME = "Bill Reminders";

    private NotificationHelper() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription("DueBuddy reminders for upcoming and due bills.");
        channel.enableVibration(true);
        channel.setShowBadge(true);

        manager.createNotificationChannel(channel);
    }

    public static void showBillReminder(Context context, Bill bill, int daysBefore,
                                        String message) {
        createChannel(context);

        DatabaseHelper db = new DatabaseHelper(context);
        String title = getTitle(daysBefore);
        int notificationId = bill.getId() * 100 + daysBefore;

        db.insertNotification(
                bill.getUserId(),
                bill.getId(),
                title,
                message
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra("bill_id", bill.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    public static void showPaidConfirmation(Context context, Bill bill) {
        createChannel(context);

        DatabaseHelper db = new DatabaseHelper(context);
        String title = "Payment Recorded";
        String message = String.format(
                Locale.US,
                "%s has been marked as paid. Amount: ₱%.2f.",
                bill.getBillName(),
                bill.getAmount()
        );

        db.insertNotification(
                bill.getUserId(),
                bill.getId(),
                title,
                message
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra("bill_id", bill.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notificationId = bill.getId() * 100 + 50;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(
                notificationId,
                builder.build()
        );
    }

    private static String getTitle(int daysBefore) {
        if (daysBefore == ReminderScheduler.DAYS_7) {
            return "Bill Due in 7 Days";
        }

        if (daysBefore == ReminderScheduler.DAYS_1) {
            return "Bill Due Tomorrow";
        }

        if (daysBefore == ReminderScheduler.DAYS_OVERDUE) {
            return "Bill Overdue";
        }

        return "Bill Due Today";
    }
}
