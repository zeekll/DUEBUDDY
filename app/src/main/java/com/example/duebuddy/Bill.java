package com.example.duebuddy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Bill {

    private final int id;
    private int userId;
    private final String billName;
    private final String accountHolder;
    private final String category;
    private final double amount;
    private final String dueDate;
    private final int recurring;
    private final String startDate;
    private final String endDate;
    private final String notes;
    private String status;

    public Bill(int id,
                String billName,
                String accountHolder,
                String category,
                double amount,
                String dueDate,
                int recurring,
                String startDate,
                String endDate,
                String notes,
                String status) {

        this.id = id;
        this.billName = billName;
        this.accountHolder = accountHolder;
        this.category = category;
        this.amount = amount;
        this.dueDate = dueDate;
        this.recurring = recurring;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBillName() {
        return billName;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public int getRecurring() {
        return recurring;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayStatus() {
        if ("Paid".equalsIgnoreCase(status)) {
            return "Paid";
        }

        Date due = parseDate(dueDate);

        if (due == null) {
            return "Unpaid";
        }

        Calendar dueDay = Calendar.getInstance();
        dueDay.setTime(due);
        clearTime(dueDay);

        Calendar today = Calendar.getInstance();
        clearTime(today);

        if (dueDay.before(today)) {
            return "Overdue";
        }

        if (dueDay.equals(today)) {
            return "Due Today";
        }

        return "Upcoming";
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
                return new SimpleDateFormat(format, Locale.US).parse(value);
            } catch (ParseException ignored) {
                // Try the next format.
            }
        }

        return null;
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
