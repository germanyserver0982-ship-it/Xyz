package com.zoya.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Local, always-listening trigger for the word "Zoya".
 *
 * HONEST LIMITATION: true low-power hotword detection (the kind that runs continuously for days
 * on a DSP without draining the battery) needs a dedicated trained keyword model — e.g. Picovoice
 * Porcupine, with a custom "Zoya.ppn" wake-word file trained on their console. That's the
 * production-grade path and is a drop-in replacement for this class (same start()/stop()/
 * onWake() surface).
 *
 * What this class does instead, using only stock Android APIs as requested, is run Android's
 * on-device SpeechRecognizer (SpeechRecognizer.isOnDeviceRecognitionAvailable) in short,
 * continuously-restarting listening windows and checks partial results for "zoya". It is
 * genuinely local/offline where the device supports on-device recognition, but it is heavier on
 * battery than a real DSP hotword engine and slightly less reliable in noisy environments — treat
 * it as a working placeholder to swap for Porcupine before shipping.
 */
class WakeWordDetector(
    private val context: Context,
    private val wakeWord: String = "zoya",
    private val onWake: () -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun start() {
        if (listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("WakeWordDetector", "No speech recognizer available on this device")
            return
        }
        listening = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        launchListeningWindow()
    }

    fun stop() {
        listening = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun launchListeningWindow() {
        if (!listening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }
        recognizer?.startListening(intent)
    }

    private fun containsWakeWord(text: String?): Boolean =
        text?.lowercase()?.contains(wakeWord) == true

    private val listener = object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle) {
            val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches?.any { containsWakeWord(it) } == true) {
                onWake()
            }
        }

        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches?.any { containsWakeWord(it) } == true) {
                onWake()
            }
            // Immediately relisten to stay "always on" in the background.
            if (listening) launchListeningWindow()
        }

        override fun onError(error: Int) {
            // No speech / timeout / busy — just restart the window and keep listening.
            if (listening) launchListeningWindow()
        }

        override fun onEndOfSpeech() {
            if (listening) launchListeningWindow()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
