package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moto_ai_prefs", Context.MODE_PRIVATE)

    data class PreferencesState(
        val language: String = "auto",
        val speechSpeed: Float = 1.0f,
        val speechPitch: Float = 1.0f,
        val autoSpeak: Boolean = true,
        val videoResolution: String = "720p",
        val wakeWordEnabled: Boolean = false,
        val floatingEnabled: Boolean = false,
        val customApiKey: String = ""
    )

    private val _state = MutableStateFlow(loadPreferences())
    val state: StateFlow<PreferencesState> = _state.asStateFlow()

    private fun loadPreferences(): PreferencesState {
        return PreferencesState(
            language = prefs.getString("language", "auto") ?: "auto",
            speechSpeed = prefs.getFloat("speech_speed", 1.0f),
            speechPitch = prefs.getFloat("speech_pitch", 1.0f),
            autoSpeak = prefs.getBoolean("auto_speak", true),
            videoResolution = prefs.getString("video_resolution", "720p") ?: "720p",
            wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false),
            floatingEnabled = prefs.getBoolean("floating_enabled", false),
            customApiKey = prefs.getString("custom_api_key", "") ?: ""
        )
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _state.value = _state.value.copy(language = lang)
    }

    fun setSpeechSpeed(speed: Float) {
        prefs.edit().putFloat("speech_speed", speed).apply()
        _state.value = _state.value.copy(speechSpeed = speed)
    }

    fun setSpeechPitch(pitch: Float) {
        prefs.edit().putFloat("speech_pitch", pitch).apply()
        _state.value = _state.value.copy(speechPitch = pitch)
    }

    fun setAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean("auto_speak", enabled).apply()
        _state.value = _state.value.copy(autoSpeak = enabled)
    }

    fun setVideoResolution(res: String) {
        prefs.edit().putString("video_resolution", res).apply()
        _state.value = _state.value.copy(videoResolution = res)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
        _state.value = _state.value.copy(wakeWordEnabled = enabled)
    }

    fun setFloatingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_enabled", enabled).apply()
        _state.value = _state.value.copy(floatingEnabled = enabled)
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        _state.value = _state.value.copy(customApiKey = key)
    }
}
