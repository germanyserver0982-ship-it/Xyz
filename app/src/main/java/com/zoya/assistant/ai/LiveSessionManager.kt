package com.zoya.assistant.ai

import android.content.Context
import android.util.Log
import com.zoya.assistant.data.SettingsRepository
import com.zoya.assistant.tools.ToolDeclarations
import com.zoya.assistant.tools.ToolResult
import com.zoya.assistant.util.PcmPlayer
import com.zoya.assistant.util.PcmRecorder
import com.zoya.assistant.util.fromBase64
import com.zoya.assistant.util.rmsAmplitude
import com.zoya.assistant.util.toBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * Owns the bidirectional WebSocket connection to Gemini's Live API (BidiGenerateContent), plus
 * the mic capture / speaker playback loops that feed and drain it, and the running text
 * transcript (for the Chat screen). Isolated from BackgroundAudioService (Android lifecycle) and
 * ToolExecutionEngine (what a tool call actually does) — this class only owns the live connection.
 *
 * IMPLEMENTATION NOTE: Google ships the Live API's official client as the JS/Node `@google/genai`
 * SDK; there is no equivalent first-party Kotlin/Android SDK with Live support as of this
 * writing. This class talks to the same underlying WebSocket protocol directly (the
 * `BidiGenerateContent` endpoint) over OkHttp, which is the standard approach for native Live API
 * clients on Android/iOS/desktop today.
 *
 * The API key is no longer baked into the build — it's read from SettingsRepository (DataStore),
 * i.e. whatever the user typed into the in-app Settings screen.
 */
class LiveSessionManager(
    private val context: Context,
    private val onToolCall: (name: String, args: JsonObject) -> ToolResult
) {
    companion object {
        private const val TAG = "LiveSessionManager"

        // Swap this if/when your API key's project resolves a different Live-capable model name.
        private const val MODEL = "models/gemini-3.1-flash-live-preview"
        private const val WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val settings = SettingsRepository(context)
    private val scope = CoroutineScope(SupervisorJob())
    private var micJob: Job? = null
    private var webSocket: WebSocket? = null
    private val player = PcmPlayer()
    private val recorder = PcmRecorder()

    private val _state = MutableStateFlow(ZoyaState.IDLE)
    val state: StateFlow<ZoyaState> = _state

    /** 0f..1f amplitude of whichever audio stream is currently active — drives the orb waveform. */
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    /** Running conversation transcript (speech-to-text on both sides) for the Chat screen. */
    private val _transcript = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val transcript: StateFlow<List<ConversationMessage>> = _transcript

    /** Set when connect() fails, e.g. no API key configured yet — Settings/UI screens observe this. */
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // long-lived streaming socket
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var userIsSpeaking = false
    private val pendingUserText = StringBuilder()
    private val pendingModelText = StringBuilder()

    fun connect() {
        if (webSocket != null) return
        _state.value = ZoyaState.CONNECTING
        _connectionError.value = null

        scope.launch {
            val apiKey = settings.apiKeyFlow.first()
            if (apiKey.isBlank()) {
                _connectionError.value = "No Gemini API key set yet — add one in Settings."
                _state.value = ZoyaState.IDLE
                return@launch
            }
            val voiceName = settings.voiceNameFlow.first()
            openSocket(apiKey, voiceName)
        }
    }

    private fun openSocket(apiKey: String, voiceName: String) {
        val request = Request.Builder().url("$WS_BASE?key=$apiKey").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send(buildSetupMessage(voiceName))
                player.prepare()
                startMicStreaming(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Live socket failure", t)
                _connectionError.value = when (response?.code) {
                    401, 403 -> "That API key was rejected — double check it in Settings."
                    else -> "Couldn't connect to Zoya. Check your connection and API key."
                }
                _state.value = ZoyaState.IDLE
                disconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _state.value = ZoyaState.IDLE
            }
        })
    }

    fun disconnect() {
        micJob?.cancel()
        micJob = null
        webSocket?.close(1000, "client closing")
        webSocket = null
        player.release()
        _state.value = ZoyaState.IDLE
    }

    /** Lets the Chat screen's text field send a typed turn instead of / alongside voice. */
    fun sendText(message: String) {
        if (message.isBlank()) return
        appendTranscript(ConversationMessage.Role.USER, message)

        fun push() {
            val msg = buildJsonObject {
                putJsonObject("clientContent") {
                    putJsonArray("turns") {
                        addJsonObject {
                            put("role", "user")
                            putJsonArray("parts") {
                                addJsonObject { put("text", message) }
                            }
                        }
                    }
                    put("turnComplete", true)
                }
            }
            webSocket?.send(msg.toString())
            _state.value = ZoyaState.THINKING
        }

        if (webSocket == null) {
            // Not connected yet — connect first, then send once the socket is open.
            scope.launch {
                val apiKey = settings.apiKeyFlow.first()
                if (apiKey.isBlank()) {
                    _connectionError.value = "No Gemini API key set yet — add one in Settings."
                    return@launch
                }
                val voiceName = settings.voiceNameFlow.first()
                _state.value = ZoyaState.CONNECTING
                val request = Request.Builder().url("$WS_BASE?key=$apiKey").build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        ws.send(buildSetupMessage(voiceName))
                        player.prepare()
                        startMicStreaming(ws)
                        push()
                    }

                    override fun onMessage(ws: WebSocket, text: String) = handleServerMessage(text)
                    override fun onMessage(ws: WebSocket, bytes: ByteString) = handleServerMessage(bytes.utf8())
                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        _connectionError.value = "Couldn't connect to Zoya. Check your connection and API key."
                        _state.value = ZoyaState.IDLE
                        disconnect()
                    }
                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        _state.value = ZoyaState.IDLE
                    }
                })
            }
        } else {
            push()
        }
    }

    // ---- Outgoing: setup + mic streaming -----------------------------------------------------

    private fun buildSetupMessage(voiceName: String): String = buildJsonObject {
        putJsonObject("setup") {
            put("model", MODEL)
            putJsonObject("generationConfig") {
                putJsonArray("responseModalities") { add("AUDIO") }
                putJsonObject("speechConfig") {
                    putJsonObject("voiceConfig") {
                        putJsonObject("prebuiltVoiceConfig") {
                            put("voiceName", voiceName)
                        }
                    }
                }
            }
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    addJsonObject { put("text", GeminiPersona.SYSTEM_INSTRUCTION) }
                }
            }
            putJsonArray("tools") {
                addJsonObject {
                    put("functionDeclarations", Json.parseToJsonElement(ToolDeclarations.FUNCTION_DECLARATIONS))
                }
            }
            // Ask the server to transcribe both sides of the call as text so the Chat screen has
            // something to render — the session itself still stays fully audio-to-audio.
            putJsonObject("inputAudioTranscription") {}
            putJsonObject("outputAudioTranscription") {}
        }
    }.toString()

    private fun startMicStreaming(ws: WebSocket) {
        micJob = scope.launch {
            recorder.start().collect { chunk ->
                _amplitude.value = chunk.rmsAmplitude()
                if (_amplitude.value > 0.02f && !userIsSpeaking) {
                    userIsSpeaking = true
                    _state.value = ZoyaState.LISTENING
                    player.interrupt() // barge-in: cut Zoya off the instant the user talks
                }
                val msg = buildJsonObject {
                    putJsonObject("realtimeInput") {
                        putJsonArray("mediaChunks") {
                            addJsonObject {
                                put("mimeType", "audio/pcm;rate=16000")
                                put("data", chunk.toBase64())
                            }
                        }
                    }
                }
                ws.send(msg.toString())
            }
        }
    }

    // ---- Incoming: audio playback, transcription, turn state, tool calls ---------------------

    private fun handleServerMessage(text: String) {
        val json = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return

        json["serverContent"]?.jsonObject?.let { content ->
            val modelTurn = content["modelTurn"]?.jsonObject
            val audioParts = modelTurn?.get("parts")?.jsonArray.orEmpty()
            if (audioParts.isNotEmpty()) {
                _state.value = ZoyaState.SPEAKING
                for (part in audioParts) {
                    val inline = part.jsonObject["inlineData"]?.jsonObject ?: continue
                    val data = inline["data"]?.jsonPrimitive?.contentOrNull ?: continue
                    val pcm = data.fromBase64()
                    _amplitude.value = pcm.rmsAmplitude()
                    player.write(pcm)
                }
            }

            content["inputTranscription"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?.let { pendingUserText.append(it) }

            content["outputTranscription"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?.let { pendingModelText.append(it) }

            if (content["interrupted"]?.jsonPrimitive?.booleanOrNull == true) {
                player.interrupt()
                _state.value = ZoyaState.LISTENING
            }

            if (content["turnComplete"]?.jsonPrimitive?.booleanOrNull == true) {
                flushTranscripts()
                userIsSpeaking = false
                _state.value = ZoyaState.IDLE
                _amplitude.value = 0f
            }
        }

        json["toolCall"]?.jsonObject?.let { toolCall ->
            _state.value = ZoyaState.THINKING
            val calls = toolCall["functionCalls"]?.jsonArray.orEmpty()
            val responses = buildJsonArray {
                for (call in calls) {
                    val obj = call.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull
                    val args = obj["args"]?.jsonObject ?: JsonObject(emptyMap())
                    val result = onToolCall(name, args)
                    addJsonObject {
                        id?.let { put("id", it) }
                        put("name", name)
                        putJsonObject("response") {
                            put("result", toolResultMessage(result))
                        }
                    }
                }
            }
            webSocket?.send(
                buildJsonObject { put("toolResponse", buildJsonObject { put("functionResponses", responses) }) }.toString()
            )
        }
    }

    private fun flushTranscripts() {
        if (pendingUserText.isNotBlank()) {
            appendTranscript(ConversationMessage.Role.USER, pendingUserText.toString().trim())
        }
        if (pendingModelText.isNotBlank()) {
            appendTranscript(ConversationMessage.Role.ZOYA, pendingModelText.toString().trim())
        }
        pendingUserText.clear()
        pendingModelText.clear()
    }

    private fun appendTranscript(role: ConversationMessage.Role, text: String) {
        _transcript.value = _transcript.value + ConversationMessage(role = role, text = text)
    }

    private fun toolResultMessage(result: ToolResult): String = when (result) {
        is ToolResult.Success -> result.message
        is ToolResult.MissingPermission -> "PERMISSION_DENIED: ${result.friendlyReason}"
        is ToolResult.Failure -> "FAILED: ${result.reason}"
    }
}
