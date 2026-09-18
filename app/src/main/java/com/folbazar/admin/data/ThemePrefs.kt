package com.folbazar.admin.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Persists the admin's dark/light theme choice so it's remembered between app launches. */
object ThemePrefs {
    private const val PREFS = "folbazar_theme"
    private const val KEY_MODE = "mode"

    enum class Mode { SYSTEM, LIGHT, DARK }

    var mode by mutableStateOf(Mode.SYSTEM)
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val saved = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, Mode.SYSTEM.name)
        mode = runCatching { Mode.valueOf(saved ?: Mode.SYSTEM.name) }.getOrDefault(Mode.SYSTEM)
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit { putString(KEY_MODE, newMode.name) }
    }
}
