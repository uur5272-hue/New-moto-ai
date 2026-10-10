package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MotoWakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListeningLoop = false

    companion object {
        const val CHANNEL_ID = "moto_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.moto.ACTION_STOP_WAKE_WORD"
        const val EXTRA_TRIGGERED_BY_WAKE_WORD = "extra_triggered_by_wake_word"
        const val EXTRA_WAKE_TRIGGERED = EXTRA_TRIGGERED_BY_WAKE_WORD

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()
        val isRunning = isServiceRunning

        fun start(context: Context) {
            val intent = Intent(context, MotoWakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MotoWakeWordService::class.java)
            context.stopService(intent)
        }

        fun startService(context: Context) = start(context)
        fun stopService(context: Context) = stop(context)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        _isServiceRunning.value = true
        startWakeWordRecognition()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MOTO AI Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening service for 'Hey MOTO' wake word"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MotoWakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MOTO AI is Listening")
            .setContentText("Say \"Hey MOTO\" or \"MOTO\" to activate")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingAppIntent)
            .addAction(0, "Stop", pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startWakeWordRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        isListeningLoop = true
        initAndListen()
    }

    private fun initAndListen() {
        if (!isListeningLoop) return

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isListeningLoop) {
                        handler.postDelayed({ initAndListen() }, 1500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.lowercase() ?: ""
                    checkWakeWord(spoken)
                    if (isListeningLoop) {
                        handler.postDelayed({ initAndListen() }, 800)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.lowercase() ?: ""
                    checkWakeWord(spoken)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            if (isListeningLoop) {
                handler.postDelayed({ initAndListen() }, 2000)
            }
        }
    }

    private fun checkWakeWord(spokenText: String) {
        val keywords = listOf("hey moto", "moto", "hello moto", "sun moto", "ok moto")
        if (keywords.any { spokenText.contains(it) }) {
            // Trigger activation
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_TRIGGERED_BY_WAKE_WORD, true)
            }
            startActivity(launchIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isListeningLoop = false
        _isServiceRunning.value = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
        speechRecognizer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
