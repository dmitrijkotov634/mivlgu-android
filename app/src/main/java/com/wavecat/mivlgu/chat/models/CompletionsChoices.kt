package com.wavecat.mivlgu.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsChoices(
    val message: Message
)