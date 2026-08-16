package com.zoya.assistant.util

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Gemini Live expects 16kHz, 16-bit, mono PCM input and returns 24kHz, 16-bit, mono PCM output.
 * These constants and wrappers keep that contract in one place so LiveSessionManager and
 * BackgroundAudioService never have to think about the raw AudioRecord/AudioTrack APIs.
 */
object AudioConfig {
    const val INPUT_SAMPLE_RATE = 16_000
    const val OUTPUT_SAMPLE_RATE = 24_000
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}

/** Wraps AudioRecord as a cold Flow of raw PCM16 chunks, one emission per read. */
class PcmRecorder {

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Flow<ByteArray> = callbackFlow {
        val minBuf = AudioRecord.getMinBufferSize(
            AudioConfig.INPUT_SAMPLE_RATE, AudioConfig.CHANNEL_IN, AudioConfig.ENCODING
        )
        val bufferSize = maxOf(minBuf, 4096)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            AudioConfig.INPUT_SAMPLE_RATE,
            AudioConfig.CHANNEL_IN,
            AudioConfig.ENCODING,
            bufferSize
        )
        recorder.startRecording()

        val buffer = ByteArray(bufferSize)
        var running = true
        val thread = Thread {
            while (running) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    trySend(buffer.copyOf(read))
                }
            }
        }
        thread.start()

        awaitClose {
            running = false
            recorder.stop()
            recorder.release()
        }
    }
}

/** Thin wrapper around AudioTrack for streaming Zoya's spoken output as it arrives. */
class PcmPlayer {
    private var track: AudioTrack? = null

    fun prepare() {
        val minBuf = AudioTrack.getMinBufferSize(
            AudioConfig.OUTPUT_SAMPLE_RATE, AudioConfig.CHANNEL_OUT, AudioConfig.ENCODING
        )
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioConfig.OUTPUT_SAMPLE_RATE)
                    .setEncoding(AudioConfig.ENCODING)
                    .setChannelMask(AudioConfig.CHANNEL_OUT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, 4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()
    }

    /** Feed a PCM16 chunk to be played immediately. */
    fun write(pcm: ByteArray) {
        track?.write(pcm, 0, pcm.size)
    }

    /** Called on user interruption (barge-in) — stop playback instantly and flush buffered audio. */
    fun interrupt() {
        track?.pause()
        track?.flush()
        track?.play()
    }

    fun release() {
        track?.stop()
        track?.release()
        track = null
    }
}

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

/** Simple RMS-based amplitude estimate (0f..1f) used to drive the waveform animation. */
fun ByteArray.rmsAmplitude(): Float {
    if (isEmpty()) return 0f
    var sum = 0.0
    var i = 0
    while (i < size - 1) {
        val sample = ((this[i + 1].toInt() shl 8) or (this[i].toInt() and 0xFF)).toShort()
        sum += sample * sample
        i += 2
    }
    val samples = size / 2
    if (samples == 0) return 0f
    val rms = kotlin.math.sqrt(sum / samples)
    return (rms / 32768.0).coerceIn(0.0, 1.0).toFloat()
}
