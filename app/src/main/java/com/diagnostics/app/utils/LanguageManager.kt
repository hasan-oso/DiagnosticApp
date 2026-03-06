/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OSTİM Technical University
 */
package com.diagnostics.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LanguageManager {
    enum class Language(val code: String, val displayName: String, val flag: String) {
        TURKISH("tr", "Türkçe", "🇹🇷"),
        ARABIC("ar", "العربية", "🇸🇦"),
        ENGLISH("en", "English", "🇬🇧")
    }

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLanguage(context: Context, language: Language) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun getLanguage(context: Context): Language {
        val savedName = getPrefs(context).getString(KEY_LANGUAGE, Language.ARABIC.name)
        return try {
            Language.valueOf(savedName ?: Language.ARABIC.name)
        } catch (e: Exception) {
            Language.ARABIC
        }
    }

    /**
     * Wraps the given context with the saved locale.
     * Call this from Activity.attachBaseContext() to ensure
     * all stringResource() calls use the user's chosen language.
     */
    fun wrapContext(context: Context): Context {
        val language = getLanguage(context)
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}
