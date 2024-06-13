package com.wavecat.mivlgu.ui

import android.os.Parcelable
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import kotlinx.parcelize.Parcelize
import java.util.Calendar

sealed interface TimetableInfo {
    @Parcelize
    data class Success(
        val timetable: List<TimetableItem>,
        val isEven: Boolean,
        val todayIndex: Int,
        val currentWeek: Int?,
        val disableFilter: Boolean,
        val disableWeekClasses: Boolean,
        val startDate: Calendar?,
        val hasInvalidRanges: Boolean,
        val showCurrentWeek: Boolean
    ) : Parcelable, TimetableInfo

    data class Failure(
        val title: String,
        val message: String
    ) : TimetableInfo
}

