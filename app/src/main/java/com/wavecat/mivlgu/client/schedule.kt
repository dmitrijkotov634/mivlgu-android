@file:Suppress("SpellCheckingInspection")

package com.wavecat.mivlgu.client

import android.os.Parcelable
import com.wavecat.mivlgu.ParaExtraData
import kotlinx.parcelize.Parcelize
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
    val semestr: String = "",
    val year: String = "",
    val disciplines: Map<String, Map<String, Map<String, List<Para>>>> = mapOf(),
)

@Serializable
enum class Status {
    @SerialName("error")
    ERROR,

    @SerialName("ok")
    OK,
}

@Serializable
enum class WeekType {
    @SerialName("even")
    EVEN,

    @SerialName("odd")
    ODD,

    @SerialName("all")
    ALL
}

@Parcelize
@Serializable
data class Para(
    val idDay: Int,
    val numberPara: Int,
    val discipline: String,
    val type: String,
    val typeWeek: WeekType,
    val aud: String,
    val numberWeek: String,
    val comment: String,
    val zaoch: Boolean? = null,
    val name: String = "",
    val groupName: String = "",
    val countFieldsLabs: String? = null,
    val underGroup: String? = null,
    @SerialName("under_group_1")
    val underGroup1: String? = null,
    @SerialName("under_group_2")
    val underGroup2: String? = null,
    var extraData: ParaExtraData? = null
) : Parcelable

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