package com.example.duebuddy;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "DueBuddySession";
    private static final String USER_ID = "user_id";
    private static final String USERNAME = "username";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(int userId, String username) {
        preferences.edit()
                .putInt(USER_ID, userId)
                .putString(USERNAME, username)
                .apply();
    }

    public int getUserId() {
        return preferences.getInt(USER_ID, -1);
    }

    public String getUsername() {
        return preferences.getString(USERNAME, "");
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}
