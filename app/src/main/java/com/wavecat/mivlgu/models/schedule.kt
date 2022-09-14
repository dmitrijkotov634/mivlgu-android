package com.wavecat.mivlgu.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
@OptIn(ExperimentalSerializationApi::class)
enum class WeekType {
    @JsonNames("even")
    EVEN,

    @JsonNames("odd")
    ODD,

    @JsonNames("all")
    ALL
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Para(
    @JsonNames("id_day") val idDay: Int,
    @JsonNames("number_para") val numberPara: Int,
    val discipline: String,
    val type: String,
    @JsonNames("type_week") val typeWeek: WeekType,
    val aud: String,
    @JsonNames("number_week") val numberWeek: String,
    val comment: String,
    val zaoch: Boolean? = null,
    val name: String = "",
    @JsonNames("group_name") val groupName: String = "",
    @JsonNames("count_fields_labs") val countFieldsLabs: String? = null,
    @JsonNames("under_group") val underGroup: String? = null
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