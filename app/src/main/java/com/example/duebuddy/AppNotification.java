package com.example.duebuddy;

public class AppNotification {

    private final int id;
    private final int billId;
    private final String title;
    private final String message;
    private final long createdAt;
    private final boolean read;

    public AppNotification(int id, int billId, String title, String message,
                           long createdAt, boolean read) {
        this.id = id;
        this.billId = billId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public int getBillId() {
        return billId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
