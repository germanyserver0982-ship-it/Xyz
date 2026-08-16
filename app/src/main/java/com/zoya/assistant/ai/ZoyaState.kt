package com.zoya.assistant.ai

/** Drives both the orb animation and the foreground service notification text. */
enum class ZoyaState {
    IDLE,       // Wake-word listener running, no live session open
    CONNECTING, // Live session handshake in progress
    LISTENING,  // Session open, streaming mic audio, user has the floor
    THINKING,   // Waiting on model turnComplete after user stopped talking
    SPEAKING    // Zoya's audio is playing back
}
