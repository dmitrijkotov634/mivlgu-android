package com.wavecat.mivlgu.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType
import com.wavecat.mivlgu.chat.models.CompletionsInput
import com.wavecat.mivlgu.chat.models.CompletionsResult
import com.wavecat.mivlgu.chat.models.Message
import com.wavecat.mivlgu.chat.plugins.Defaults
import com.wavecat.mivlgu.chat.plugins.Plugin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.math.min

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val messages: MutableList<Message> = mutableListOf()

    private val registry = Encodings.newLazyEncodingRegistry()
    val encoding by lazy { registry.getEncodingForModel(ModelType.GPT_3_5_TURBO)!! }

    data class Event(
        val messages: MutableList<Message>,
        val isNewMessage: Boolean
    )

    private val _event = MutableLiveData(Event(messages, false))
    private val _messageInputText = MutableLiveData("")
    private val _isLoading = MutableLiveData(false)
    private val _noInternetConnection = MutableLiveData(false)

    val event: LiveData<Event> = _event
    val messageInputText: LiveData<String> = _messageInputText
    val isLoading: LiveData<Boolean> = _isLoading
    val noInternetConnection: LiveData<Boolean> = _noInternetConnection

    private var plugins: List<Plugin> = listOf(
        Defaults(),

        //LinkReader(encoding)
    )

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            url("https://mivlgu.dmitrijkotov634.workers.dev/chat")
            contentType(ContentType.Application.Json)
        }
    }

    fun deleteMessage(position: Int) {
        messages.removeAt(position)
        _event.value = Event(messages, false)
    }

    fun sendMessage(text: String) {
        _isLoading.value = true

        val userMessage = Message(Message.USER, text)
        messages.add(userMessage)
        _event.value = Event(messages, true)

        viewModelScope.launch(Dispatchers.IO) {
            val systemMessage = StringBuilder()

            plugins.forEach { plugin ->
                plugin.onPreProcessMessage(userMessage)?.let {
                    systemMessage.append(it)
                    if (it.isNotBlank())
                        systemMessage.append("\n")
                }
            }

            val systemMessageString = systemMessage.toString()
            val systemTokens = encoding.encode(systemMessageString).size
            val tokenLimit = MAX_TOKENS - min(systemTokens, MAX_TOKENS)
            val (inputMessages, _) = trimMessages(messages, tokenLimit)
            val inputMessagesMutable = inputMessages.toMutableList().apply {
                add(0, Message(Message.SYSTEM, systemMessageString))
            }

            try {
                val response: CompletionsResult = client.post {
                    setBody(
                        CompletionsInput(
                            "gpt-3.5-turbo",
                            inputMessagesMutable
                        )
                    )
                }.body()

                val message = response.choices[0].message
                messages.add(message)
                plugins.forEach { it.onPostProcessMessage(message) }
                _event.postValue(Event(messages, true))
            } catch (e: IOException) {
                _noInternetConnection.postValue(true)
            } catch (e: Exception) {
                messages.add(Message(Message.ERROR, e.message.toString()))
                _event.postValue(Event(messages, true))
            }

            delay(3000)

            _isLoading.postValue(false)
        }
    }

    fun saveInputText(text: String) {
        _messageInputText.value = text
    }

    fun clear() {
        messages.clear()
        _event.value = Event(messages, false)

        viewModelScope.launch(Dispatchers.IO) {
            plugins.forEach { it.onClearContext() }
        }
    }

    private fun trimMessages(
        inputMessages: List<Message>,
        tokenLimit: Int
    ): Pair<List<Message>, Int> {
        val resultMessages = mutableListOf<Message>()

        var numTokens = 0

        for (message in inputMessages.reversed()) {
            if (message.isInternalRole()) continue

            numTokens += 6
            numTokens += encoding.encode(message.role).size
            numTokens += encoding.encode(message.content).size

            if (numTokens > tokenLimit) {
                if (resultMessages.isEmpty())
                    resultMessages.add(
                        message.copy(
                            content = encoding.decode(
                                encoding.encode(message.content)
                                    .take(tokenLimit)
                            )
                        )
                    )

                break
            }

            resultMessages.add(message)
        }

        return resultMessages.reversed() to numTokens
    }

    companion object {
        const val MAX_TOKENS = 3096
    }
}