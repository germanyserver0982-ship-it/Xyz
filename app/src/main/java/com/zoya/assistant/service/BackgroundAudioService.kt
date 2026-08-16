package com.zoya.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zoya.assistant.MainActivity
import com.zoya.assistant.R
import com.zoya.assistant.ai.LiveSessionManager
import com.zoya.assistant.ai.ZoyaState
import com.zoya.assistant.tools.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps Zoya alive in the background: owns the wake-word listener when idle, and owns the live
 * Gemini session once woken, so the whole pipeline survives the user leaving the app / screen
 * turning off. UI (MainActivity) binds to this service to observe state rather than owning any
 * of this logic itself — that's the Clean Architecture boundary: this service + LiveSessionManager
 * + ToolExecutionEngine form the "background/data" layer; Compose screens are purely presentation.
 */
class BackgroundAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "zoya_assistant_channel"
        const val NOTIF_ID = 42
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob())
    private var wakeWordDetector: WakeWordDetector? = null
    lateinit var sessionManager: LiveSessionManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundAudioService = this@BackgroundAudioService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(ZoyaState.IDLE))

        sessionManager = LiveSessionManager(applicationContext) { name, args ->
            // Tool calls execute on the service's context so they keep working even if the
            // Activity has been backgrounded or destroyed.
            com.zoya.assistant.tools.ToolExecutionEngine(applicationContext).execute(name, args)
        }

        scope.launch {
            sessionManager.state.collect { state ->
                updateNotification(state)
                if (state == ZoyaState.IDLE) {
                    // Session ended (turn complete / closed) — go back to listening for the wake word.
                    startWakeWordListening()
                }
            }
        }

        startWakeWordListening()
    }

    private fun startWakeWordListening() {
        if (wakeWordDetector != null) return
        wakeWordDetector = WakeWordDetector(applicationContext) {
            wakeWordDetector?.stop()
            wakeWordDetector = null
            sessionManager.connect()
        }
        wakeWordDetector?.start()
    }

    /** Called from the UI for manual "hold to talk" style activation, bypassing the wake word. */
    fun wakeManually() {
        wakeWordDetector?.stop()
        wakeWordDetector = null
        sessionManager.connect()
    }

    fun endConversation() {
        sessionManager.disconnect()
        startWakeWordListening()
    }

    override fun onDestroy() {
        wakeWordDetector?.stop()
        sessionManager.disconnect()
        scope.cancel()
        super.onDestroy()
    }

    // ---- Notification --------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notif_channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: ZoyaState) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.notif_title))
        .setContentText(
            if (state == ZoyaState.IDLE) getString(R.string.notif_text_idle)
            else getString(R.string.notif_text_active)
        )
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun updateNotification(state: ZoyaState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(state))
    }
}
