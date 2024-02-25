package com.wavecat.mivlgu.ui.chat.plugins

import com.wavecat.mivlgu.ui.chat.models.Message


interface Plugin {
    suspend fun onPostProcessMessage(assistantMessage: Message)

    suspend fun onPreProcessMessage(userMessage: Message): String?

    suspend fun onClearContext()
}