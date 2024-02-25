package com.wavecat.mivlgu.ui.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsChoices(
    val message: Message
)