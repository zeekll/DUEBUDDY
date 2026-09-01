package com.example.duebuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserDatabase.db";

    // INCREASE VERSION
    private static final int DATABASE_VERSION = 11;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ============================================================
    // DATABASE CREATION
    // ============================================================

    @Override
    public void onCreate(SQLiteDatabase db) {

        // USERS
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE, " +
                        "password TEXT, " +
                        "phone_number TEXT" +
                        ")"
        );

        // BILLS
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS bills (" +
                        "bill_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "bill_name TEXT, " +
                        "account_holder TEXT, " +
                        "category TEXT, " +
                        "amount REAL, " +
                        "due_date TEXT, " +
                        "recurring INTEGER, " +
                        "start_date TEXT, " +
                        "end_date TEXT, " +
                        "notes TEXT, " +
                        "status TEXT, " +
                        "overdue_notified INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );

        createNotificationsTable(db);
        createIncomeTable(db);
        createBudgetTables(db);
    }

    // ============================================================
    // DATABASE UPGRADE
    // ============================================================

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        // --------------------------------------------------------
        // OLD VERSION FIXES
        // --------------------------------------------------------

        if (oldVersion < 4) {

            if (tableExists(db, "bills")
                    && !columnExists(db, "bills", "user_id")) {

                db.execSQL(
                        "ALTER TABLE bills " +
                                "ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0"
                );

                db.execSQL(
                        "UPDATE bills SET user_id = " +
                                "(SELECT id FROM users ORDER BY id LIMIT 1) " +
                                "WHERE user_id = 0"
                );
            }
        }

        if (oldVersion < 5) {
            createNotificationsTable(db);
        }

        if (oldVersion < 6) {

            if (tableExists(db, "bills")
                    && !columnExists(
                    db,
                    "bills",
                    "overdue_notified")) {

                db.execSQL(
                        "ALTER TABLE bills " +
                                "ADD COLUMN overdue_notified " +
                                "INTEGER NOT NULL DEFAULT 0"
                );
            }
        }

        if (oldVersion < 7) {
            createIncomeTable(db);
        }

        if (oldVersion < 8) {
            createBudgetTables(db);
        }

        // --------------------------------------------------------
        // BUDGET ITEMS FIX
        // --------------------------------------------------------

        if (oldVersion < 9) {

            db.execSQL(
                    "DROP TABLE IF EXISTS budget_items"
            );

            createBudgetItemsTable(db);
        }

        // --------------------------------------------------------
        // VERSION 10
        // --------------------------------------------------------

        if (oldVersion < 10) {

            createMonthlyBudgetTable(db);

            if (!tableExists(db, "budget_items")
                    || !columnExists(
                    db,
                    "budget_items",
                    "item_id")) {

                db.execSQL(
                        "DROP TABLE IF EXISTS budget_items"
                );

                createBudgetItemsTable(db);
            }
        }

        // --------------------------------------------------------
        // VERSION 11
        //
        // IMPORTANT:
        // Make sure monthly_income exists.
        // --------------------------------------------------------

        if (oldVersion < 11) {

            createIncomeTable(db);
            createMonthlyBudgetTable(db);
            createBudgetItemsTable(db);
            createNotificationsTable(db);
        }

        // --------------------------------------------------------
        // FINAL SAFETY CHECK
        //
        // Even if the database came from an older/broken version,
        // make sure the required tables exist.
        // --------------------------------------------------------

        createNotificationsTable(db);
        createIncomeTable(db);
        createMonthlyBudgetTable(db);
        createBudgetItemsTable(db);
    }

    // ============================================================
    // TABLE CHECK
    // ============================================================

    private boolean tableExists(
            SQLiteDatabase db,
            String tableName) {

        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master " +
                        "WHERE type='table' AND name=?",
                new String[]{tableName}
        );

        try {

            return cursor.moveToFirst();

        } finally {

            cursor.close();
        }
    }

    // ============================================================
    // COLUMN CHECK
    // ============================================================

    private boolean columnExists(
            SQLiteDatabase db,
            String table,
            String column) {

        if (!tableExists(db, table)) {
            return false;
        }

        Cursor cursor = db.rawQuery(
                "PRAGMA table_info(" + table + ")",
                null
        );

        try {

            int nameIndex =
                    cursor.getColumnIndex("name");

            while (cursor.moveToNext()) {

                if (column.equals(
                        cursor.getString(nameIndex))) {

                    return true;
                }
            }

        } finally {

            cursor.close();
        }

        return false;
    }

    // ============================================================
    // NOTIFICATIONS TABLE
    // ============================================================

    private void createNotificationsTable(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS notifications (" +
                        "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "bill_id INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "message TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "is_read INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );
    }

    // ============================================================
    // MONTHLY INCOME TABLE
    // ============================================================

    private void createIncomeTable(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS monthly_income (" +
                        "income_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "source TEXT NOT NULL, " +
                        "month TEXT NOT NULL, " +
                        "year INTEGER NOT NULL" +
                        ")"
        );
    }

    // ============================================================
    // MONTHLY BUDGET TABLE
    // ============================================================

    private void createMonthlyBudgetTable(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS monthly_budget (" +
                        "budget_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER UNIQUE, " +
                        "amount REAL NOT NULL" +
                        ")"
        );
    }

    // ============================================================
    // BUDGET ITEMS TABLE
    // ============================================================

    private void createBudgetItemsTable(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS budget_items (" +
                        "item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "description TEXT NOT NULL, " +
                        "amount REAL NOT NULL" +
                        ")"
        );
    }

    // ============================================================
    // BUDGET TABLES
    // ============================================================

    private void createBudgetTables(
            SQLiteDatabase db) {

        createMonthlyBudgetTable(db);
        createBudgetItemsTable(db);
    }

    // ============================================================
    // USER METHODS
    // ============================================================

    public int insertUserAndGetId(
            String username,
            String password,
            String phone) {

        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("password", password);
        values.put("phone_number", phone);

        long result =
                getWritableDatabase().insert(
                        "users",
                        null,
                        values
                );

        return result < 0
                ? -1
                : (int) result;
    }

    public boolean insertUser(
            String username,
            String password,
            String phone) {

        return insertUserAndGetId(
                username,
                password,
                phone
        ) != -1;
    }

    public Cursor getUserByCredentials(
            String username,
            String password) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM users " +
                        "WHERE username = ? AND password = ?",
                new String[]{
                        username,
                        password
                }
        );
    }

    public Cursor getUser(int userId) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM users WHERE id = ?",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }

    public String getUserPhoneNumber(
            int userId) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT phone_number FROM users " +
                                "WHERE id = ?",
                        new String[]{
                                String.valueOf(userId)
                        }
                );

        try {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }

        } finally {

            cursor.close();
        }

        return "";
    }

    public boolean updateUser(
            int userId,
            String username,
            String password,
            String phone) {

        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("password", password);
        values.put("phone_number", phone);

        return getWritableDatabase().update(
                "users",
                values,
                "id = ?",
                new String[]{
                        String.valueOf(userId)
                }
        ) > 0;
    }

    // ============================================================
    // BILL METHODS
    // ============================================================

    public int insertBillAndGetId(
            int userId,
            String billName,
            String accountHolder,
            String category,
            double amount,
            String dueDate,
            int recurring,
            String startDate,
            String endDate,
            String notes) {

        ContentValues values =
                createBillValues(
                        userId,
                        billName,
                        accountHolder,
                        category,
                        amount,
                        dueDate,
                        recurring,
                        startDate,
                        endDate,
                        notes
                );

        values.put("status", "Unpaid");

        long result =
                getWritableDatabase().insert(
                        "bills",
                        null,
                        values
                );

        return result < 0
                ? -1
                : (int) result;
    }

    public boolean updateBill(
            int billId,
            int userId,
            String billName,
            String accountHolder,
            String category,
            double amount,
            String dueDate,
            int recurring,
            String startDate,
            String endDate,
            String notes) {

        ContentValues values =
                createBillValues(
                        userId,
                        billName,
                        accountHolder,
                        category,
                        amount,
                        dueDate,
                        recurring,
                        startDate,
                        endDate,
                        notes
                );

        values.put("overdue_notified", 0);

        return getWritableDatabase().update(
                "bills",
                values,
                "bill_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(billId),
                        String.valueOf(userId)
                }
        ) > 0;
    }

    private ContentValues createBillValues(
            int userId,
            String billName,
            String accountHolder,
            String category,
            double amount,
            String dueDate,
            int recurring,
            String startDate,
            String endDate,
            String notes) {

        ContentValues values =
                new ContentValues();

        values.put("user_id", userId);
        values.put("bill_name", billName);
        values.put("account_holder", accountHolder);
        values.put("category", category);
        values.put("amount", amount);
        values.put("due_date", dueDate);
        values.put("recurring", recurring);
        values.put("start_date", startDate);
        values.put("end_date", endDate);
        values.put("notes", notes);

        return values;
    }

    public ArrayList<Bill> getBillsList(
            int userId) {

        ArrayList<Bill> bills =
                new ArrayList<>();

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT * FROM bills " +
                                "WHERE user_id = ? " +
                                "ORDER BY due_date ASC, bill_id DESC",
                        new String[]{
                                String.valueOf(userId)
                        }
                );

        try {

            while (cursor.moveToNext()) {
                bills.add(
                        createBillFromCursor(cursor)
                );
            }

        } finally {

            cursor.close();
        }

        return bills;
    }

    public ArrayList<Bill>
    getAllBillsForReminders() {

        ArrayList<Bill> bills =
                new ArrayList<>();

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT * FROM bills " +
                                "WHERE status IS NULL OR status != ?",
                        new String[]{"Paid"}
                );

        try {

            while (cursor.moveToNext()) {

                bills.add(
                        createBillFromCursor(cursor)
                );
            }

        } finally {

            cursor.close();
        }

        return bills;
    }

    public Bill getBill(
            int billId,
            int userId) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT * FROM bills " +
                                "WHERE bill_id = ? AND user_id = ?",
                        new String[]{
                                String.valueOf(billId),
                                String.valueOf(userId)
                        }
                );

        try {

            if (cursor.moveToFirst()) {

                return createBillFromCursor(
                        cursor
                );
            }

        } finally {

            cursor.close();
        }

        return null;
    }

    private Bill createBillFromCursor(
            Cursor cursor) {

        Bill bill = new Bill(
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "bill_id"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "bill_name"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "account_holder"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "category"
                        )
                ),
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "amount"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "due_date"
                        )
                ),
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "recurring"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "start_date"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "end_date"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "notes"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "status"
                        )
                )
        );

        bill.setUserId(
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "user_id"
                        )
                )
        );

        return bill;
    }

    public boolean markBillPaid(
            int billId,
            int userId) {

        ContentValues values =
                new ContentValues();

        values.put("status", "Paid");

        return getWritableDatabase().update(
                "bills",
                values,
                "bill_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(billId),
                        String.valueOf(userId)
                }
        ) > 0;
    }

    public boolean deleteBill(
            int billId,
            int userId) {

        return getWritableDatabase().delete(
                "bills",
                "bill_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(billId),
                        String.valueOf(userId)
                }
        ) > 0;
    }

    public int countBills(
            int userId) {

        return count(
                "user_id = ?",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }

    public int countPaidBills(
            int userId) {

        return count(
                "user_id = ? AND status = 'Paid'",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }

    public int countUnpaidBills(
            int userId) {

        return count(
                "user_id = ? AND status != 'Paid'",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }

    private int count(
            String selection,
            String[] arguments) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT COUNT(*) FROM bills " +
                                "WHERE " + selection,
                        arguments
                );

        try {

            return cursor.moveToFirst()
                    ? cursor.getInt(0)
                    : 0;

        } finally {

            cursor.close();
        }
    }

    // ============================================================
    // OVERDUE NOTIFICATIONS
    // ============================================================

    public boolean isOverdueNotificationSent(
            int billId,
            int userId) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT overdue_notified FROM bills " +
                                "WHERE bill_id = ? AND user_id = ?",
                        new String[]{
                                String.valueOf(billId),
                                String.valueOf(userId)
                        }
                );

        try {

            return cursor.moveToFirst()
                    && cursor.getInt(0) == 1;

        } finally {

            cursor.close();
        }
    }

    public void markOverdueNotificationSent(
            int billId,
            int userId) {

        ContentValues values =
                new ContentValues();

        values.put(
                "overdue_notified",
                1
        );

        getWritableDatabase().update(
                "bills",
                values,
                "bill_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(billId),
                        String.valueOf(userId)
                }
        );
    }

    // ============================================================
    // NOTIFICATION METHODS
    // ============================================================

    public long insertNotification(
            int userId,
            int billId,
            String title,
            String message) {

        ContentValues values =
                new ContentValues();

        values.put("user_id", userId);
        values.put("bill_id", billId);
        values.put("title", title);
        values.put("message", message);
        values.put(
                "created_at",
                System.currentTimeMillis()
        );
        values.put("is_read", 0);

        return getWritableDatabase().insert(
                "notifications",
                null,
                values
        );
    }

    public ArrayList<AppNotification>
    getNotifications(int userId) {

        ArrayList<AppNotification>
                notifications =
                new ArrayList<>();

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT * FROM notifications " +
                                "WHERE user_id = ? " +
                                "ORDER BY created_at DESC, notification_id DESC",
                        new String[]{
                                String.valueOf(userId)
                        }
                );

        try {

            while (cursor.moveToNext()) {

                notifications.add(
                        createNotificationFromCursor(
                                cursor
                        )
                );
            }

        } finally {

            cursor.close();
        }

        return notifications;
    }

    public int getUnreadNotificationCount(
            int userId) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT COUNT(*) FROM notifications " +
                                "WHERE user_id = ? AND is_read = 0",
                        new String[]{
                                String.valueOf(userId)
                        }
                );

        try {

            return cursor.moveToFirst()
                    ? cursor.getInt(0)
                    : 0;

        } finally {

            cursor.close();
        }
    }

    public void markNotificationRead(
            int notificationId,
            int userId) {

        ContentValues values =
                new ContentValues();

        values.put("is_read", 1);

        getWritableDatabase().update(
                "notifications",
                values,
                "notification_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(notificationId),
                        String.valueOf(userId)
                }
        );
    }

    public void markAllNotificationsRead(
            int userId) {

        ContentValues values =
                new ContentValues();

        values.put("is_read", 1);

        getWritableDatabase().update(
                "notifications",
                values,
                "user_id = ?",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }

    public boolean deleteNotification(
            int notificationId,
            int userId) {

        return getWritableDatabase().delete(
                "notifications",
                "notification_id = ? AND user_id = ?",
                new String[]{
                        String.valueOf(notificationId),
                        String.valueOf(userId)
                }
        ) > 0;
    }

    private AppNotification
    createNotificationFromCursor(
            Cursor cursor) {

        return new AppNotification(
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "notification_id"
                        )
                ),
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "bill_id"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "title"
                        )
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "message"
                        )
                ),
                cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                "created_at"
                        )
                ),
                cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "is_read"
                        )
                ) == 1
        );
    }

    // ============================================================
    // MONTHLY INCOME
    // ============================================================

    public boolean saveMonthlyIncome(
            int userId,
            double amount,
            String source,
            String month,
            int year) {

        SQLiteDatabase db =
                getWritableDatabase();

        db.delete(
                "monthly_income",
                "user_id = ? AND month = ? AND year = ?",
                new String[]{
                        String.valueOf(userId),
                        month,
                        String.valueOf(year)
                }
        );

        ContentValues values =
                new ContentValues();

        values.put("user_id", userId);
        values.put("amount", amount);
        values.put("source", source);
        values.put("month", month);
        values.put("year", year);

        long result =
                db.insert(
                        "monthly_income",
                        null,
                        values
                );

        return result != -1;
    }

    public Cursor getMonthlyIncome(
            int userId,
            String month,
            int year) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM monthly_income " +
                        "WHERE user_id = ? " +
                        "AND month = ? " +
                        "AND year = ?",
                new String[]{
                        String.valueOf(userId),
                        month,
                        String.valueOf(year)
                }
        );
    }

    public double getCurrentMonthlyIncome(
            int userId,
            String month,
            int year) {

        Cursor cursor =
                getMonthlyIncome(
                        userId,
                        month,
                        year
                );

        try {

            if (cursor.moveToFirst()) {

                return cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "amount"
                        )
                );
            }

        } finally {

            cursor.close();
        }

        return 0;
    }

    public String getMonthlyIncomeSource(
            int userId,
            String month,
            int year) {

        Cursor cursor =
                getMonthlyIncome(
                        userId,
                        month,
                        year
                );

        try {

            if (cursor.moveToFirst()) {

                return cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "source"
                        )
                );
            }

        } finally {

            cursor.close();
        }

        return "";
    }

    // ============================================================
    // MONTHLY BUDGET
    // ============================================================

    public void saveMonthlyBudget(
            int userId,
            double amount) {

        SQLiteDatabase db =
                getWritableDatabase();

        ContentValues values =
                new ContentValues();

        values.put("user_id", userId);
        values.put("amount", amount);

        db.insertWithOnConflict(
                "monthly_budget",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public double getMonthlyBudget(
            int userId) {

        Cursor cursor =
                getReadableDatabase().rawQuery(
                        "SELECT amount FROM monthly_budget " +
                                "WHERE user_id = ?",
                        new String[]{
                                String.valueOf(userId)
                        }
                );

        try {

            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }

        } finally {

            cursor.close();
        }

        return 0.0;
    }

    // ============================================================
    // BUDGET ITEMS
    // ============================================================

    public long insertBudgetItem(
            int userId,
            String category,
            String description,
            double amount) {

        ContentValues values =
                new ContentValues();

        values.put("user_id", userId);
        values.put("category", category);
        values.put("description", description);
        values.put("amount", amount);

        return getWritableDatabase().insert(
                "budget_items",
                null,
                values
        );
    }

    public Cursor getBudgetItems(
            int userId) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM budget_items " +
                        "WHERE user_id = ? " +
                        "ORDER BY item_id DESC",
                new String[]{
                        String.valueOf(userId)
                }
        );
    }
}