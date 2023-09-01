package com.wavecat.mivlgu.chat.plugins

import com.wavecat.mivlgu.chat.models.Message


interface Plugin {
    suspend fun onPostProcessMessage(assistantMessage: Message)

    suspend fun onPreProcessMessage(userMessage: Message): String?

    suspend fun onClearContext()
}