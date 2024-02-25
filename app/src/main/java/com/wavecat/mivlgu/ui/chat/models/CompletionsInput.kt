package com.wavecat.mivlgu.ui.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsInput(
    val model: String,
    val messages: List<Message>
)