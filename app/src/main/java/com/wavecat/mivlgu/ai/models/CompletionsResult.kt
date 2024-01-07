package com.wavecat.mivlgu.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionsResult(
    val choices: List<CompletionsChoices>
)