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
) : Parcelable {
    @IgnoredOnParcel
    val numberWeekParsed by lazy { parseEnumeration(numberWeek) }

    @IgnoredOnParcel
    val underGroup1Parsed by lazy { parseEnumeration(underGroup1 ?: "") }

    @IgnoredOnParcel
    val underGroup2Parsed by lazy { parseEnumeration(underGroup2 ?: "") }

    fun isToday(weekType: WeekType, currentWeek: Int): Boolean {
        if (numberWeekParsed.isToday(weekType, currentWeek))
            return true

        return (underGroup1Parsed.isToday(weekType, currentWeek) || underGroup2Parsed.isToday(weekType, currentWeek))
    }
}