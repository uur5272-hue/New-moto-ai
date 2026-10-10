package com.example

import android.app.Application
import com.example.control.DeviceControlManager
import com.example.data.db.MotoDatabase
import com.example.data.repository.UserPreferencesRepository
import com.example.media.MediaStorageManager
import com.example.network.GeminiApiClient
import com.example.voice.MotoVoiceManager

class MotoApplication : Application() {

    companion object {
        lateinit var instance: MotoApplication
            private set
    }

    val database by lazy { MotoDatabase.getInstance(this) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val geminiApiClient by lazy { GeminiApiClient(preferencesRepository) }
    val mediaStorageManager by lazy { MediaStorageManager(this) }
    val deviceControlManager by lazy { DeviceControlManager(this) }
    val voiceManager by lazy { MotoVoiceManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
