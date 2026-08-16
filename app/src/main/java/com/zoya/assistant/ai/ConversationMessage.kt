package com.zoya.assistant.ai

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ZOYA }
}
