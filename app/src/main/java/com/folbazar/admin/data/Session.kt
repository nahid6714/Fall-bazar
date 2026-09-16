package com.folbazar.admin.data

import android.content.Context
import androidx.core.content.edit

/** Holds the current Supabase Auth session in memory and persists it to SharedPreferences
 *  so the admin stays logged in between app launches. */
object Session {
    private const val PREFS = "folbazar_session"

    var accessToken: String? = null
        private set
    var email: String? = null
        private set
    var userId: String? = null
        private set

    val isLoggedIn: Boolean get() = accessToken != null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = p.getString("access_token", null)
        email = p.getString("email", null)
        userId = p.getString("user_id", null)
    }

    fun save(context: Context, token: String, email: String, userId: String) {
        accessToken = token
        this.email = email
        this.userId = userId
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("access_token", token)
            putString("email", email)
            putString("user_id", userId)
        }
    }

    fun clear(context: Context) {
        accessToken = null
        email = null
        userId = null
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}
