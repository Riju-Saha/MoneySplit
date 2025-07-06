package com.example.moneysplit

import android.content.Context
import androidx.core.content.edit

object SessionManager {
    private const val PREF_NAME = "UserSession"
    private const val KEY_LOGGED_IN = "isLoggedIn"
    private const val KEY_USERNAME = "loggedInUsername"

    fun saveUserSession(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun login(ctx: Context, username: String) {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun logout(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

    fun isLoggedIn(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    fun getLoggedInUsername(ctx: Context): String? {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, null)
    }
}
