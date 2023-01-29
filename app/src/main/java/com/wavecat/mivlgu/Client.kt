package com.wavecat.mivlgu

import com.wavecat.mivlgu.models.ScheduleGetResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*

class Client {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun scheduleGetJson(group: String, semester: String, year: String): ScheduleGetResult =
        client.get {
            url("$baseUrl?r=schedulecash/group")
            parameter("group", group)
            parameter("semester", semester)
            parameter("year", year)
            parameter("format", "json")
        }.body()

    suspend fun scheduleGetTeacherJson(
        teacherId: Int,
        semester: String,
        year: String
    ): ScheduleGetResult =
        client.get {
            url("$baseUrl?r=schedulecash/teacher")
            parameter("teacher_id", teacherId)
            parameter("semester", semester)
            parameter("year", year)
            parameter("format", "json")
        }.body()

    companion object {
        const val baseUrl = "https://scala.mivlgu.ru/core/frontend/index.php"
    }
}