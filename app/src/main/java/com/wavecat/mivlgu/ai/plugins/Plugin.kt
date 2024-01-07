package com.wavecat.mivlgu.ai.plugins

import com.wavecat.mivlgu.ai.models.Message


interface Plugin {
    suspend fun onPostProcessMessage(assistantMessage: Message)

    suspend fun onPreProcessMessage(userMessage: Message): String?

    suspend fun onClearContext()
}