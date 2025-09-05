package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit().putString("username", value).apply()

    var isSuperAdmin: Boolean
        get() = prefs.getBoolean("is_super_admin", false)
        set(value) = prefs.edit().putBoolean("is_super_admin", value).apply()

    var tokenIssuedAtMs: Long
        get() = prefs.getLong("token_issued_at_ms", 0L)
        set(value) = prefs.edit().putLong("token_issued_at_ms", value).apply()

    fun isLoggedIn(validDurationMs: Long = 30L * 24 * 60 * 60 * 1000) : Boolean {
        val t = token
        val issued = tokenIssuedAtMs
        if (t.isNullOrBlank() || issued <= 0L) return false
        val now = System.currentTimeMillis()
        return (now - issued) <= validDurationMs
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}


