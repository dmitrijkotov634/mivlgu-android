package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wavecat.mivlgu.chat.ChatClient
import com.wavecat.mivlgu.chat.models.CompletionsInput
import com.wavecat.mivlgu.chat.models.Message
import com.wavecat.mivlgu.tokenizer.GPT2Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenizer by lazy { GPT2Tokenizer(application.assets) }

    private val messages: MutableList<Message> = default.toMutableList()

    data class Event(
        val messages: MutableList<Message>
    )

    val event = MutableLiveData(Event(messages))
    val isLoading = MutableLiveData(false)

    private val client = ChatClient()

    fun sendMessage(text: String) {
        isLoading.value = true

        messages.add(Message("user", text))
        event.value = Event(messages)

        viewModelScope.launch(Dispatchers.IO) {
            cleanup()

            try {
                val response = client.makeCompletion(
                    CompletionsInput(
                        "gpt-3.5-turbo",
                        messages.filter {
                            !it.isInternalRole()
                        }
                    )
                )

                messages.add(response.choices[0].message)
            } catch (e: Exception) {
                messages.add(Message("error", e.message.toString()))
            }

            event.postValue(Event(messages))
            isLoading.postValue(false)
        }
    }

    fun clear() {
        messages.clear()
        messages.addAll(default)
        event.value = Event(messages)
    }

    private fun cleanup() {
        var numTokens = 0
        for (message in messages.drop(1).reversed()) {
            if (message.isInternalRole())
                continue
            numTokens += 4
            numTokens += tokenizer.encode(message.role).size
            numTokens += tokenizer.encode(message.content).size
            if (numTokens >= maxTokens) {
                messages.remove(message)
                break
            }
            numTokens += 2
        }
    }

    companion object {
        const val maxTokens = 3500

        private val default = listOf(
            Message(
                "system",
                "Ты ассистент приложения для Муромского института МИВлГУ"
            )
        )
    }
}