package com.wavecat.mivlgu.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    var content: String
) {
    fun isInternalRole(): Boolean = role == SERVER || role == ERROR

    fun isAssistantRole(): Boolean = role == ASSISTANT

    fun isUserRole(): Boolean = role == USER

    companion object {
        const val SYSTEM = "system"
        const val ERROR = "error"
        const val SERVER = "server"
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}