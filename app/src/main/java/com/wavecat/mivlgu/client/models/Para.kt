package com.wavecat.mivlgu.client.models

import android.os.Parcelable
import com.wavecat.mivlgu.ParaExtraData
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Para(
    val idDay: Int,
    val numberPara: Int,
    val discipline: String,
    val type: String,
    val typeWeek: WeekType,
    @SerialName("aud")
    val audience: String,
    @SerialName("number_week")
    val weekNumber: String,
    val comment: String,
    val zaoch: Boolean? = null,
    val name: String = "",
    val groupName: String = "",
    val countFieldsLabs: String? = null,
    @SerialName("under_group")
    val subGroup: String? = null,
    @SerialName("under_group_1")
    val subGroup1: String? = null,
    @SerialName("under_group_2")
    val subGroup2: String? = null,
    var extraData: ParaExtraData? = null
) : Parcelable {
    @IgnoredOnParcel
    val parsedWeekNumber by lazy {
        if (weekNumber.isBlank()) WeekEnumeration.EMPTY else parseEnumeration(weekNumber)
    }

    @IgnoredOnParcel
    val parsedSubGroup1 by lazy {
        if (subGroup1.isNullOrBlank()) WeekEnumeration.EMPTY else parseEnumeration(subGroup1)
    }

    @IgnoredOnParcel
    val parsedSubGroup2 by lazy {
        if (subGroup2.isNullOrBlank()) WeekEnumeration.EMPTY else parseEnumeration(subGroup2)
    }

    fun isLessonToday(weekNumber: Int): Boolean =
        fetchTodayStatus(weekNumber) != TodayStatus.NOT_TODAY

    enum class TodayStatus {
        FOR_ALL,
        FOR_FIRST_SUBGROUP,
        FOR_SECOND_SUBGROUP,
        NOT_TODAY
    }

    fun fetchTodayStatus(weekNumber: Int): TodayStatus =
        if (
            parsedWeekNumber.isEmpty() &&
            parsedSubGroup1.isEmpty() &&
            parsedSubGroup2.isEmpty()
        )
            TodayStatus.FOR_ALL
        else when {
            parsedWeekNumber.isLessonToday(typeWeek, weekNumber) -> TodayStatus.FOR_ALL
            parsedSubGroup1.isLessonToday(typeWeek, weekNumber) -> TodayStatus.FOR_FIRST_SUBGROUP
            parsedSubGroup2.isLessonToday(typeWeek, weekNumber) -> TodayStatus.FOR_SECOND_SUBGROUP
            else -> TodayStatus.NOT_TODAY
        }
}