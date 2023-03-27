package com.wavecat.mivlgu.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wavecat.mivlgu.chat.models.CompletionsInput
import com.wavecat.mivlgu.chat.models.CompletionsResult
import com.wavecat.mivlgu.chat.models.Message
import com.wavecat.mivlgu.tokenizer.GPT2Tokenizer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenizer by lazy { GPT2Tokenizer(application.assets) }

    private val messages: MutableList<Message> = default.toMutableList()

    data class Event(
        val messages: MutableList<Message>
    )

    val event = MutableLiveData(Event(messages))
    val isLoading = MutableLiveData(false)

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            url("https://noisy-disk-8ba5.dmitrijkotov634.workers.dev/chat/")
            contentType(ContentType.Application.Json)
            header(
                "sign",
                "Я использую API Дмитрия Котова t.me/wavecat, без его согласия"
            )
        }
    }

    fun sendMessage(text: String) {
        isLoading.value = true

        messages.add(Message("user", text))
        event.value = Event(messages)

        viewModelScope.launch(Dispatchers.IO) {
            cleanup()

            try {
                val response: CompletionsResult = client.post {
                    setBody(CompletionsInput(
                        "gpt-3.5-turbo",
                        messages.filter {
                            !it.isInternalRole()
                        }
                    ))
                }.body()

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
            numTokens += 2
        }

        for (message in messages.drop(1)) {
            if (message.isInternalRole())
                continue
            if (numTokens < maxTokens)
                break
            numTokens -= 4
            numTokens -= tokenizer.encode(message.role).size
            numTokens -= tokenizer.encode(message.content).size
            numTokens -= 2
            messages.remove(message)
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