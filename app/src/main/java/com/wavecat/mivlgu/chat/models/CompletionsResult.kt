package com.wavecat.mivlgu.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsResult(
    val choices: List<CompletionsChoices>
)