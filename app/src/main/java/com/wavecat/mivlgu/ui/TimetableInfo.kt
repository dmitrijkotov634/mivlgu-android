package com.wavecat.mivlgu.ui

import android.os.Parcelable
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class TimetableInfo(
    val timetable: List<TimetableItem>,
    val isEven: Boolean,
    val todayIndex: Int,
    val currentWeek: Int?,
    val disableFilter: Boolean,
    val disableWeekClasses: Boolean,
    val startDate: Calendar?,
    val hasInvalidRanges: Boolean
) : Parcelable