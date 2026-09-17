package com.folbazar.admin.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Holds the current Supabase Auth session in memory and persists it encrypted between app launches. */
object Session {
    private const val PREFS = "folbazar_session"

    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EMAIL = "email"
    private const val KEY_USER_ID = "user_id"

    var accessToken: String? = null
        private set
    var email: String? = null
        private set
    var userId: String? = null
        private set
    var refreshToken: String? = null
        private set

    private var appContext: Context? = null
    private var encryptedPrefs: android.content.SharedPreferences? = null

    val isLoggedIn: Boolean get() = accessToken != null

    private fun prefs(context: Context): android.content.SharedPreferences {
        if (encryptedPrefs == null) {
            val applicationContext = context.applicationContext
            appContext = applicationContext
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                applicationContext,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return encryptedPrefs!!
    }

    fun init(context: Context) {
        val p = prefs(context)
        accessToken = p.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = p.getString(KEY_REFRESH_TOKEN, null)
        email = p.getString(KEY_EMAIL, null)
        userId = p.getString(KEY_USER_ID, null)
    }

    fun save(
        context: Context,
        token: String,
        email: String,
        userId: String,
        refreshToken: String? = null
    ) {
        val p = prefs(context)
        accessToken = token
        this.email = email
        this.userId = userId
        this.refreshToken = refreshToken ?: this.refreshToken

        p.edit {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_EMAIL, email)
            putString(KEY_USER_ID, userId)
            this@Session.refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
        }
    }

    fun updateAccessToken(token: String, refreshToken: String? = null) {
        accessToken = token
        if (!refreshToken.isNullOrBlank()) this.refreshToken = refreshToken
        encryptedPrefs?.edit {
            putString(KEY_ACCESS_TOKEN, token)
            this@Session.refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
        }
    }

    fun clear(context: Context) {
        accessToken = null
        refreshToken = null
        email = null
        userId = null
        appContext = context.applicationContext
        prefs(context).edit { clear() }
        encryptedPrefs = null
    }
}
