package com.wavecat.mivlgu.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    var content: String
) {
    fun isInternalRole(): Boolean = role == "server" || role == "error"
}