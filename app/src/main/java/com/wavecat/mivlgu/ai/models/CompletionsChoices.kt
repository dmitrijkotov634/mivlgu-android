package com.wavecat.mivlgu.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsChoices(
    val message: Message
)