package com.example.duebuddy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public final class SmsReminderHelper {

    private SmsReminderHelper() {
    }

    public static boolean sendReminder(
            Context context,
            Bill bill,
            int daysBefore
    ) {
        if (!hasSmsPermission(context)) {
            return false;
        }

        DatabaseHelper db = new DatabaseHelper(context);

        String phoneNumber =
                db.getUserPhoneNumber(bill.getUserId());

        if (phoneNumber == null
                || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String message =
                buildReminderMessage(
                        bill,
                        daysBefore
                );

        try {
            SmsManager smsManager =
                    getSmsManager(context);

            if (smsManager == null) {
                return false;
            }

            ArrayList<String> parts =
                    smsManager.divideMessage(message);

            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(
                        phoneNumber.trim(),
                        null,
                        parts,
                        null,
                        null
                );
            } else {
                smsManager.sendTextMessage(
                        phoneNumber.trim(),
                        null,
                        message,
                        null,
                        null
                );
            }

            return true;

        } catch (SecurityException
                 | IllegalArgumentException
                 | UnsupportedOperationException exception) {

            return false;
        }
    }

    private static boolean hasSmsPermission(
            Context context
    ) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private static SmsManager getSmsManager(
            Context context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(
                    SmsManager.class
            );
        }

        return SmsManager.getDefault();
    }

    private static String buildReminderMessage(
            Bill bill,
            int daysBefore
    ) {
        String timing;

        if (daysBefore == ReminderScheduler.DAYS_7) {
            timing = "in 7 days";
        } else if (daysBefore == ReminderScheduler.DAYS_1) {
            timing = "tomorrow";
        } else if (daysBefore == ReminderScheduler.DAYS_OVERDUE) {
            timing = "overdue";
        } else {
            timing = "today";
        }

        return String.format(
                Locale.US,
                "DueBuddy Reminder: Your %s bill of ₱%.2f is due %s. Due date: %s.",
                bill.getBillName(),
                bill.getAmount(),
                timing,
                bill.getDueDate()
        );
    }
}
