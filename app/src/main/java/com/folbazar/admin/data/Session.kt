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

    val isLoggedIn: Boolean get() = accessToken != null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = p.getString("access_token", null)
        email = p.getString("email", null)
    }

    fun save(context: Context, token: String, email: String) {
        accessToken = token
        this.email = email
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("access_token", token)
            putString("email", email)
        }
    }

    fun clear(context: Context) {
        accessToken = null
        email = null
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}
