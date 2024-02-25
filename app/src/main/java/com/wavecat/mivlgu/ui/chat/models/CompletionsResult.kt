package com.wavecat.mivlgu.ui.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsResult(
    val choices: List<CompletionsChoices>
)