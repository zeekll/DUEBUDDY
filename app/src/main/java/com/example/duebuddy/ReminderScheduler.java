package com.example.duebuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderScheduler {

    public static final int DAYS_7 = 7;
    public static final int DAYS_1 = 1;
    public static final int DAYS_TODAY = 0;
    public static final int DAYS_OVERDUE = -1;

    private static final int REMINDER_HOUR = 9;
    private static final long TODAY_DELAY_MILLIS = 1_000L;

    private ReminderScheduler() {
    }

    public static void schedule(Context context, Bill bill) {
        cancel(context, bill.getId());

        if ("Paid".equalsIgnoreCase(bill.getStatus())) {
            return;
        }

        Date dueDate = parseDate(bill.getDueDate());

        if (dueDate == null) {
            return;
        }

        long now = System.currentTimeMillis();

        scheduleDaysBefore(
                context,
                bill,
                DAYS_7,
                dueDate,
                now
        );

        scheduleDaysBefore(
                context,
                bill,
                DAYS_1,
                dueDate,
                now
        );

        scheduleToday(
                context,
                bill,
                dueDate,
                now
        );

        scheduleOverdue(
                context,
                bill,
                dueDate,
                now
        );
    }

    private static void scheduleDaysBefore(
            Context context,
            Bill bill,
            int daysBefore,
            Date dueDate,
            long now
    ) {
        Calendar target = Calendar.getInstance();
        target.setTime(dueDate);
        clearTime(target);

        target.add(
                Calendar.DAY_OF_YEAR,
                -daysBefore
        );

        target.set(
                Calendar.HOUR_OF_DAY,
                REMINDER_HOUR
        );

        long triggerTime = target.getTimeInMillis();

        if (isToday(target)) {
            if (triggerTime <= now) {
                triggerTime = now + 60_000L;
            }

            setAlarm(
                    context,
                    bill,
                    daysBefore,
                    triggerTime
            );

            return;
        }

        if (triggerTime > now) {
            setAlarm(
                    context,
                    bill,
                    daysBefore,
                    triggerTime
            );
        }
    }

    private static void scheduleToday(
            Context context,
            Bill bill,
            Date dueDate,
            long now
    ) {
        Calendar dueDay = Calendar.getInstance();
        dueDay.setTime(dueDate);
        clearTime(dueDay);

        Calendar today = Calendar.getInstance();
        clearTime(today);

        if (!isSameDay(dueDay, today)) {
            return;
        }

        // Bills due today are announced immediately after they are saved.
        long triggerTime = now + TODAY_DELAY_MILLIS;

        setAlarm(
                context,
                bill,
                DAYS_TODAY,
                triggerTime
        );
    }

    private static void scheduleOverdue(
            Context context,
            Bill bill,
            Date dueDate,
            long now
    ) {
        Calendar overdueTime = Calendar.getInstance();
        overdueTime.setTime(dueDate);
        clearTime(overdueTime);

        overdueTime.add(
                Calendar.DAY_OF_YEAR,
                1
        );

        overdueTime.set(
                Calendar.HOUR_OF_DAY,
                REMINDER_HOUR
        );

        long triggerTime = overdueTime.getTimeInMillis();

        /*
         * If the bill is already overdue when it is saved,
         * show the overdue notification immediately.
         */
        if (triggerTime <= now) {
            triggerTime = now + TODAY_DELAY_MILLIS;
        }

        setAlarm(
                context,
                bill,
                DAYS_OVERDUE,
                triggerTime
        );
    }

    private static void setAlarm(
            Context context,
            Bill bill,
            int daysBefore,
            long triggerTime
    ) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(
                context,
                BillReminderReceiver.class
        );

        intent.putExtra(
                BillReminderReceiver.EXTRA_BILL_ID,
                bill.getId()
        );

        intent.putExtra(
                BillReminderReceiver.EXTRA_USER_ID,
                bill.getUserId()
        );

        intent.putExtra(
                BillReminderReceiver.EXTRA_DAYS_BEFORE,
                daysBefore
        );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode(
                                bill.getId(),
                                daysBefore
                        ),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            return;
        }

        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );
    }

    public static void cancel(
            Context context,
            int billId
    ) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager == null) {
            return;
        }

        int[] reminderTypes = {
                DAYS_7,
                DAYS_1,
                DAYS_TODAY,
                DAYS_OVERDUE
        };

        for (int daysBefore : reminderTypes) {
            Intent intent = new Intent(
                    context,
                    BillReminderReceiver.class
            );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            requestCode(
                                    billId,
                                    daysBefore
                            ),
                            intent,
                            PendingIntent.FLAG_NO_CREATE
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static int requestCode(
            int billId,
            int daysBefore
    ) {
        return billId * 10 + daysBefore;
    }

    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String[] formats = {
                "yyyy-M-d",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "M/d/yyyy"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(
                                format,
                                Locale.US
                        );

                dateFormat.setLenient(false);

                return dateFormat.parse(
                        value.trim()
                );
            } catch (Exception ignored) {
                // Try the next supported format.
            }
        }

        return null;
    }

    private static boolean isToday(Calendar calendar) {
        Calendar today = Calendar.getInstance();

        return isSameDay(
                calendar,
                today
        );
    }

    private static boolean isSameDay(
            Calendar first,
            Calendar second
    ) {
        return first.get(Calendar.YEAR)
                == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR)
                == second.get(Calendar.DAY_OF_YEAR);
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );
    }
}
