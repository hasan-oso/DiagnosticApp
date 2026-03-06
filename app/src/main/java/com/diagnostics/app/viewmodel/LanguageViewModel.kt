package com.diagnostics.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.language.AppStrings
import com.diagnostics.app.utils.language.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadSavedLanguage())
    val language: StateFlow<LanguageManager.Language> = _language.asStateFlow()

    val strings: StateFlow<Strings> = _language.map { lang ->
        AppStrings.get(lang)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppStrings.get(_language.value))

    val isRtl: StateFlow<Boolean> = _language.map { lang ->
        lang == LanguageManager.Language.ARABIC
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _language.value == LanguageManager.Language.ARABIC)

    fun setLanguage(language: LanguageManager.Language) {
        _language.value = language
        // احفظ باستخدام نفس المفتاح المستخدم في LanguageManager
        prefs.edit().putString("app_language", language.name).apply()
        // أيضاً أخبر LanguageManager
        LanguageManager.saveLanguage(getApplication(), language)
    }

    private fun loadSavedLanguage(): LanguageManager.Language {
        val savedName = prefs.getString("app_language", LanguageManager.Language.ARABIC.name)
        return try {
            LanguageManager.Language.valueOf(savedName ?: LanguageManager.Language.ARABIC.name)
        } catch (e: Exception) {
            LanguageManager.Language.ARABIC
        }
    }
}
