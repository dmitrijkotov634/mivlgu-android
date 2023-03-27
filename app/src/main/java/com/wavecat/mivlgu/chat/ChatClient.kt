package com.wavecat.mivlgu.chat

import com.wavecat.mivlgu.chat.models.CompletionsInput
import com.wavecat.mivlgu.chat.models.CompletionsResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ChatClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun makeCompletion(input: CompletionsInput): CompletionsResult =
        client.post {
            url("https://noisy-disk-8ba5.dmitrijkotov634.workers.dev/chat/")
            header(
                "sign",
                "Я использую API Дмитрия Котова t.me/wavecat, без его согласия"
            )
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
}