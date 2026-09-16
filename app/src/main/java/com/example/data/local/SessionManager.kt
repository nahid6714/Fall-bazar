package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.example.data.model.Admin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the admin session (Supabase access/refresh tokens) and app configuration.
 *
 * Session tokens are stored in an [EncryptedSharedPreferences] file so they are not
 * readable in plaintext (e.g. from a rooted device or an adb backup). If the device
 * keystore is unavailable for any reason, we fall back to a regular (still backup-excluded)
 * SharedPreferences file rather than crashing the app.
 */
class SessionManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = createSecurePrefs(appContext)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentAdmin = MutableStateFlow(getSavedAdmin())
    val currentAdmin: StateFlow<Admin?> = _currentAdmin.asStateFlow()

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Extremely unlikely (corrupt keystore, etc). Degrade gracefully instead of crashing;
            // this file is still excluded from Android auto-backup (see backup_rules.xml).
            Log.e("SessionManager", "Encrypted prefs unavailable, falling back to plain prefs", e)
            context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        admin: Admin,
        expiresInSeconds: Long = DEFAULT_TOKEN_TTL_SECONDS
    ) {
        // expiresInSeconds <= 0 marks a session that never expires (e.g. the offline demo
        // session, which has no real JWT to refresh).
        val expiresAtMillis = if (expiresInSeconds <= 0) 0L else System.currentTimeMillis() + (expiresInSeconds * 1000)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRES_AT, expiresAtMillis)
            .putString(KEY_ADMIN_ID, admin.id)
            .putString(KEY_ADMIN_AUTH_ID, admin.authUserId)
            .putString(KEY_ADMIN_EMAIL, admin.email)
            .putString(KEY_ADMIN_NAME, admin.name)
            .putString(KEY_ADMIN_ROLE, admin.role)
            .putBoolean(KEY_ADMIN_ACTIVE, admin.active)
            .apply()

        _isLoggedIn.value = true
        _currentAdmin.value = admin
    }

    /** Called after a successful silent token refresh; keeps the admin profile untouched. */
    fun updateTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long = DEFAULT_TOKEN_TTL_SECONDS) {
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRES_AT, expiresAtMillis)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .remove(KEY_ADMIN_ID)
            .remove(KEY_ADMIN_AUTH_ID)
            .remove(KEY_ADMIN_EMAIL)
            .remove(KEY_ADMIN_NAME)
            .remove(KEY_ADMIN_ROLE)
            .remove(KEY_ADMIN_ACTIVE)
            .apply()

        _isLoggedIn.value = false
        _currentAdmin.value = null
    }

    fun getAccessToken(): String = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
    fun getRefreshToken(): String = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""

    /** True once the access token has expired (or is about to, within a 60s safety margin). */
    fun isAccessTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)
        if (expiresAt == 0L) return false // demo/offline sessions never expire
        return System.currentTimeMillis() >= (expiresAt - 60_000L)
    }

    fun getSavedAdmin(): Admin? {
        val email = prefs.getString(KEY_ADMIN_EMAIL, "") ?: ""
        if (email.isBlank()) return null
        return Admin(
            id = prefs.getString(KEY_ADMIN_ID, "") ?: "",
            authUserId = prefs.getString(KEY_ADMIN_AUTH_ID, "") ?: "",
            email = email,
            name = prefs.getString(KEY_ADMIN_NAME, "Admin") ?: "Admin",
            role = prefs.getString(KEY_ADMIN_ROLE, "admin") ?: "admin",
            active = prefs.getBoolean(KEY_ADMIN_ACTIVE, true)
        )
    }

    fun refreshCurrentAdminState() {
        _currentAdmin.value = getSavedAdmin()
    }

    // Dynamic config overrides for Supabase & Cloudinary (falls back to BuildConfig)
    fun getSupabaseUrl(): String {
        val stored = prefs.getString(KEY_CUSTOM_SUPABASE_URL, "") ?: ""
        if (stored.isNotBlank()) return stored
        return try {
            val buildConfigUrl = BuildConfig::class.java.getField("SUPABASE_URL").get(null) as? String
            if (!buildConfigUrl.isNullOrBlank() && !buildConfigUrl.contains("xyzcompany") && !buildConfigUrl.contains("your-project-id")) buildConfigUrl else DEFAULT_SUPABASE_URL
        } catch (_: Exception) {
            DEFAULT_SUPABASE_URL
        }
    }

    fun getSupabaseAnonKey(): String {
        val stored = prefs.getString(KEY_CUSTOM_SUPABASE_ANON_KEY, "") ?: ""
        if (stored.isNotBlank()) return stored
        return try {
            val buildConfigKey = BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as? String
            if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.contains("example_anon_key") && !buildConfigKey.contains("your-supabase-anon-key")) buildConfigKey else DEFAULT_SUPABASE_ANON_KEY
        } catch (_: Exception) {
            DEFAULT_SUPABASE_ANON_KEY
        }
    }

    fun getCloudinaryCloudName(): String {
        val stored = prefs.getString(KEY_CUSTOM_CLOUDINARY_CLOUD_NAME, "") ?: ""
        if (stored.isNotBlank()) return stored
        return try {
            val name = BuildConfig::class.java.getField("CLOUDINARY_CLOUD_NAME").get(null) as? String
            if (!name.isNullOrBlank() && name != "demo") name else DEFAULT_CLOUDINARY_NAME
        } catch (_: Exception) {
            DEFAULT_CLOUDINARY_NAME
        }
    }

    fun getCloudinaryUploadPreset(): String {
        val stored = prefs.getString(KEY_CUSTOM_CLOUDINARY_PRESET, "") ?: ""
        if (stored.isNotBlank()) return stored
        return try {
            val preset = BuildConfig::class.java.getField("CLOUDINARY_UPLOAD_PRESET").get(null) as? String
            if (!preset.isNullOrBlank() && preset != "bus_terminal_preset") preset else DEFAULT_CLOUDINARY_PRESET
        } catch (_: Exception) {
            DEFAULT_CLOUDINARY_PRESET
        }
    }

    fun updateConfig(supabaseUrl: String, anonKey: String, cloudName: String, preset: String) {
        prefs.edit()
            .putString(KEY_CUSTOM_SUPABASE_URL, supabaseUrl.trim().removeSuffix("/"))
            .putString(KEY_CUSTOM_SUPABASE_ANON_KEY, anonKey.trim())
            .putString(KEY_CUSTOM_CLOUDINARY_CLOUD_NAME, cloudName.trim())
            .putString(KEY_CUSTOM_CLOUDINARY_PRESET, preset.trim())
            .apply()
    }

    // App theme preference: "light", "dark", or "system" (default).
    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "btbd_admin_session_secure"
        private const val PREFS_NAME_FALLBACK = "btbd_admin_session"

        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_ADMIN_ID = "admin_id"
        private const val KEY_ADMIN_AUTH_ID = "admin_auth_id"
        private const val KEY_ADMIN_EMAIL = "admin_email"
        private const val KEY_ADMIN_NAME = "admin_name"
        private const val KEY_ADMIN_ROLE = "admin_role"
        private const val KEY_ADMIN_ACTIVE = "admin_active"

        private const val KEY_CUSTOM_SUPABASE_URL = "custom_supabase_url"
        private const val KEY_CUSTOM_SUPABASE_ANON_KEY = "custom_supabase_anon_key"
        private const val KEY_CUSTOM_CLOUDINARY_CLOUD_NAME = "custom_cloudinary_name"
        private const val KEY_CUSTOM_CLOUDINARY_PRESET = "custom_cloudinary_preset"
        private const val KEY_THEME_MODE = "theme_mode"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        const val DEFAULT_SUPABASE_URL = "https://qjmizppdxksowvysykrk.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_QB8ttrXx1YsY1chn3zvadw_edE0qBud"
        const val DEFAULT_CLOUDINARY_NAME = "pqkfbs6y"
        const val DEFAULT_CLOUDINARY_PRESET = "bus-terminal-bd"

        // Supabase's default GoTrue access-token lifetime is 3600s; used when a login
        // response omits expires_in for any reason.
        const val DEFAULT_TOKEN_TTL_SECONDS = 3600L

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
