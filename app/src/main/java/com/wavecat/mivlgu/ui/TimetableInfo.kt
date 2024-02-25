package com.wavecat.mivlgu.ui

import android.os.Parcelable
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimetableInfo(
    val timetable: List<TimetableItem>,
    val filteredTimetable: List<TimetableItem>,
    val isEven: Boolean,
    val todayIndex: Int,
    val currentWeek: Int?,
    val disableFilter: Boolean,
    val disableWeekClasses: Boolean
) : Parcelable