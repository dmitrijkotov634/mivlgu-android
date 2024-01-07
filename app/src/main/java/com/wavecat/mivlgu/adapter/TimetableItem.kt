package com.wavecat.mivlgu.adapter

import android.os.Parcelable
import com.wavecat.mivlgu.data.Para
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface TimetableItem : Parcelable {
    @Parcelize
    data class DayHeader(val title: Int) : TimetableItem

    @Parcelize
    data class ParaHeader(val index: Int, val isToday: Boolean) : TimetableItem

    @Parcelize
    data class ParaItem(
        val para: Para,
    ) : TimetableItem
}