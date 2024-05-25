package com.wavecat.mivlgu.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType
import com.wavecat.mivlgu.ui.TimetableInfo
import com.wavecat.mivlgu.ui.chat.models.CompletionsInput
import com.wavecat.mivlgu.ui.chat.models.CompletionsResult
import com.wavecat.mivlgu.ui.chat.models.Message
import com.wavecat.mivlgu.ui.chat.plugins.Plugin
import com.wavecat.mivlgu.ui.chat.plugins.Timetable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.rustore.sdk.remoteconfig.RemoteConfigClient
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
    val event: LiveData<Event> = _event

    private val _noInternetConnection = MutableLiveData(false)
    val noInternetConnection: LiveData<Boolean> = _noInternetConnection

    private val _messageInputText = MutableLiveData("")
    val messageInputText: LiveData<String> = _messageInputText

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _timetableInfo = MutableLiveData<String?>(null)
    val timetableInfo: LiveData<String?> = _timetableInfo

    private val timetable = Timetable(application)
    private var plugins: List<Plugin> = listOf(timetable)

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                coerceInputValues = true
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
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

            runCatching {
                _noInternetConnection.postValue(false)

                client.post {
                    RemoteConfigClient.instance
                        .getRemoteConfig()
                        .await().let { rc ->
                            url(rc.getString("ai_url"))
                            header("sign", rc.getString("ai_sign"))
                        }

                    setBody(
                        CompletionsInput(
                            "gpt-3.5-turbo",
                            inputMessagesMutable
                        )
                    )
                }.body<CompletionsResult>()
            }.onFailure {
                if (it is IOException) {
                    _noInternetConnection.postValue(true)
                    return@onFailure
                }

                messages.add(Message(Message.ERROR, it.message.toString()))
                _event.postValue(Event(messages, true))
            }.onSuccess { response ->
                val message = response.choices[0].message
                messages.add(message)
                plugins.forEach { it.onPostProcessMessage(message) }
                _event.postValue(Event(messages, true))
            }

            delay(3500)

            _isLoading.postValue(false)
        }
    }

    fun saveInputText(text: String) {
        _messageInputText.value = text
    }

    fun setupTimetableInfo(name: String?, timetableInfo: TimetableInfo?) {
        timetable.timetableInfo = timetableInfo
        _timetableInfo.postValue(name)
    }

    fun clear() {
        messages.clear()
        _event.value = Event(messages, false)

        viewModelScope.launch(Dispatchers.IO) {
            plugins.forEach { it.onClearContext() }
        }

        _timetableInfo.value = null
        timetable.timetableInfo = null
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
        const val MAX_TOKENS = 5000
    }
}