package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_studs_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(getApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isSetupCompleted = MutableStateFlow(isSetupCompleted())
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    fun isSetupCompleted(): Boolean {
        val completedInPrefs = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        val hasKey = !prefs.getString(KEY_API_KEY, null).isNull_or_blank()
        return completedInPrefs || hasKey
    }

    fun markSetupCompleted() {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
        _isSetupCompleted.value = true
    }

    fun getApiKey(): String? {
        val key = prefs.getString(KEY_API_KEY, null)
        return if (key.isNull_or_blank()) null else key
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = if (trimmed.isEmpty()) null else trimmed
        markSetupCompleted()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _selectedModel.value = model
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun saveThemeMode(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _themeMode.value = theme
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        _apiKey.value = null
        _selectedModel.value = DEFAULT_MODEL
        _themeMode.value = "SYSTEM"
        _isSetupCompleted.value = false
    }

    companion object {
        private const val KEY_SETUP_COMPLETED = "is_setup_completed"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_THEME = "theme_mode"
        const val DEFAULT_MODEL = "gemini-3.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-3.1-pro",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.6-flash"
        )
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
