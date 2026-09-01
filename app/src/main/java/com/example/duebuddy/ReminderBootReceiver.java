package com.example.duebuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/** Restores bill alarms after the phone restarts. */
public class ReminderBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            DatabaseHelper db = new DatabaseHelper(context);
            ArrayList<Bill> bills = db.getAllBillsForReminders();
            for (Bill bill : bills) {
                ReminderScheduler.schedule(context, bill);
            }
        }
    }
}
