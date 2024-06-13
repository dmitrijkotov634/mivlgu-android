@file:Suppress("SpellCheckingInspection")

package com.wavecat.mivlgu.client

import com.wavecat.mivlgu.client.models.DisciplinesForWeek
import com.wavecat.mivlgu.client.models.Group
import com.wavecat.mivlgu.client.models.Status
import com.wavecat.mivlgu.client.models.Teacher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleGetResult(
    val status: Status,
    val time: String,
    val group: Group? = null,
    val teacher: Teacher? = null,
    val title: String = "",
    val message: String = "",
    @SerialName("semestr")
    val semester: String = "",
    val year: String = "",
    val disciplines: DisciplinesForWeek = mapOf(),
)

