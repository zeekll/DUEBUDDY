package com.example.duebuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;

public class BillReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_BILL_ID = "bill_id";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_DAYS_BEFORE = "days_before";

    @Override
    public void onReceive(Context context, Intent intent) {
        int billId = intent.getIntExtra(EXTRA_BILL_ID, -1);
        int userId = intent.getIntExtra(EXTRA_USER_ID, -1);
        int daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 0);

        if (billId < 0 || userId < 0) {
            return;
        }

        DatabaseHelper db = new DatabaseHelper(context);
        Bill bill = db.getBill(billId, userId);

        if (bill == null || "Paid".equalsIgnoreCase(bill.getStatus())) {
            return;
        }

        if (daysBefore == ReminderScheduler.DAYS_OVERDUE
                && db.isOverdueNotificationSent(billId, userId)) {
            return;
        }

        String timing = getTiming(daysBefore);
        String message = String.format(
                Locale.US,
                "%s is due %s. Amount: ₱%.2f. Due date: %s.",
                bill.getBillName(),
                timing,
                bill.getAmount(),
                bill.getDueDate()
        );

        NotificationHelper.showBillReminder(
                context,
                bill,
                daysBefore,
                message
        );

        // Send the SMS separately so an SMS failure never prevents
        // the working in-app and phone notification from appearing.
        SmsReminderHelper.sendReminder(
                context,
                bill,
                daysBefore
        );

        if (daysBefore == ReminderScheduler.DAYS_OVERDUE) {
            db.markOverdueNotificationSent(billId, userId);
        }
    }

    private String getTiming(int daysBefore) {
        if (daysBefore == ReminderScheduler.DAYS_7) {
            return "in 7 days";
        }

        if (daysBefore == ReminderScheduler.DAYS_1) {
            return "tomorrow";
        }

        if (daysBefore == ReminderScheduler.DAYS_OVERDUE) {
            return "overdue";
        }

        return "today";
    }
}
