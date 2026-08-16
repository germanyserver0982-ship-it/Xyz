package com.zoya.assistant.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoya.assistant.ai.ConversationMessage
import com.zoya.assistant.ai.ZoyaState
import com.zoya.assistant.service.BackgroundAudioService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin presentation-layer adapter: binds to BackgroundAudioService and republishes its state/
 * amplitude/transcript flows for the Compose screens to render. No business logic of its own —
 * that all lives in LiveSessionManager / ToolExecutionEngine inside the service.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _zoyaState = MutableStateFlow(ZoyaState.IDLE)
    val zoyaState: StateFlow<ZoyaState> = _zoyaState

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _transcript = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val transcript: StateFlow<List<ConversationMessage>> = _transcript

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    private var service: BackgroundAudioService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as BackgroundAudioService.LocalBinder
            service = localBinder.getService()
            bound = true
            viewModelScope.launch {
                service?.sessionManager?.state?.collect { _zoyaState.value = it }
            }
            viewModelScope.launch {
                service?.sessionManager?.amplitude?.collect { _amplitude.value = it }
            }
            viewModelScope.launch {
                service?.sessionManager?.transcript?.collect { _transcript.value = it }
            }
            viewModelScope.launch {
                service?.sessionManager?.connectionError?.collect { _connectionError.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
            service = null
        }
    }

    fun startBackgroundService() {
        val context = getApplication<Application>()
        val intent = Intent(context, BackgroundAudioService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Manual tap on the orb — wakes Zoya if idle, ends the conversation if it's already active. */
    fun onOrbTapped() {
        when (_zoyaState.value) {
            ZoyaState.IDLE -> service?.wakeManually()
            ZoyaState.CONNECTING -> Unit
            else -> service?.endConversation()
        }
    }

    /** Typed message from the Chat screen — works whether or not a live session is already open. */
    fun sendText(message: String) {
        service?.sessionManager?.sendText(message)
    }

    fun endConversation() {
        service?.endConversation()
    }

    override fun onCleared() {
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        super.onCleared()
    }
}
