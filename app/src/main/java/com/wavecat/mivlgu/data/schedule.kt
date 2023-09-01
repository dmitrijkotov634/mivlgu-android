package com.wavecat.mivlgu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleGetResult(
    val status: String,
    val time: String,
    val group: Group? = null,
    val teacher: Teacher? = null,
    val semestr: String,
    val year: String,
    val disciplines: Map<String, Map<String, Map<String, List<Para>>>>,
)

@Serializable
enum class WeekType {
    @SerialName("even")
    EVEN,

    @SerialName("odd")
    ODD,

    @SerialName("all")
    ALL
}

@Serializable
data class Para(
    @SerialName("id_day")
    val idDay: Int,
    @SerialName("number_para")
    val numberPara: Int,
    val discipline: String,
    val type: String,
    @SerialName("type_week")
    val typeWeek: WeekType,
    val aud: String,
    @SerialName("number_week")
    val numberWeek: String,
    val comment: String,
    val zaoch: Boolean? = null,
    val name: String = "",
    @SerialName("group_name")
    val groupName: String = "",
    @SerialName("count_fields_labs")
    val countFieldsLabs: String? = null,
    @SerialName("under_group")
    val underGroup: String? = null,
    @SerialName("under_group_1")
    val underGroup1: String? = null,
    @SerialName("under_group_2")
    val underGroup2: String? = null
)

@Serializable
data class Group(
    val id: Int,
    val name: String,
)

@Serializable
data class Teacher(
    val id: Int,
    val name: String,
)