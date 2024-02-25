package com.wavecat.mivlgu.ui.timetable

import android.os.Parcelable
import com.wavecat.mivlgu.client.Para
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface TimetableItem : Parcelable {
    @Parcelize
    data class DayHeader(val index: Int) : TimetableItem

    @Parcelize
    data class ParaHeader(val index: Int, val isToday: Boolean) : TimetableItem

    @Parcelize
    data class ParaItem(
        val para: Para,
    ) : TimetableItem
}