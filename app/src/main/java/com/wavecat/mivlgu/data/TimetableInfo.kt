package com.wavecat.mivlgu.data

import android.os.Parcelable
import com.wavecat.mivlgu.adapter.TimetableItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimetableInfo(
    val timetable: List<TimetableItem>,
    val filteredTimetable: List<TimetableItem>,
    val isEven: Boolean,
    val currentDayIndex: Int,
    val currentWeek: Int?
) : Parcelable