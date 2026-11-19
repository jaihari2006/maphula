package com.offlineai.app.data

enum class SenderType {
    USER,
    AI,
    SYSTEM
}

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: SenderType,
    var text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val persona: Persona = Persona.KRISHNA,
    var isStreaming: Boolean = false,
    val relatedArticles: List<String> = emptyList(),
    val attachment: ChatAttachment? = null
)
