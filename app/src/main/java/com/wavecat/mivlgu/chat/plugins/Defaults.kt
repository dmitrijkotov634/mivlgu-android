package com.wavecat.mivlgu.chat.plugins

import com.wavecat.mivlgu.chat.models.Message

class Defaults : Plugin {
    override suspend fun onPostProcessMessage(assistantMessage: Message) {}

    override suspend fun onPreProcessMessage(userMessage: Message): String {
        return "Ты ассистент приложения для Муромского института МИВлГУ"
    }

    override suspend fun onClearContext() {}
}