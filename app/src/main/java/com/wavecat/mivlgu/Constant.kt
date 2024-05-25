package com.wavecat.mivlgu

import java.util.*

object Constant {
    val facultiesIds = listOf(2, 10, 4, 9, 16)

    fun getSemester(calendar: Calendar): String =
        if (calendar.get(Calendar.MONTH) < Calendar.JULY) "2" else "1"

    fun getYear(calendar: Calendar): String {
        val result = Calendar.getInstance()
        result.timeInMillis = calendar.timeInMillis
        if (getSemester(calendar) == "2")
            result.add(Calendar.YEAR, -1)
        return result.get(Calendar.YEAR).toString()
    }

    data class Time(
        val hour: Int,
        val minute: Int
    ) {
        operator fun rangeTo(time: Time) = TimeRange(this, time)

        fun toCalendar(): Calendar = Calendar.getInstance().apply {
            timeInMillis = 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
    }

    data class TimeRange(
        val start: Time,
        val end: Time
    ) {
        fun check(current: Calendar): Boolean =
            current.timeInMillis >= start.toCalendar().timeInMillis
                    && current.timeInMillis <= end.toCalendar().timeInMillis
    }

    val timeRanges: List<TimeRange> = listOf(
        Time(8, 30)..Time(10, 0),
        Time(10, 15)..Time(11, 45),
        Time(12, 30)..Time(14, 0),
        Time(14, 15)..Time(15, 45),
        Time(16, 0)..Time(17, 30),
        Time(17, 45)..Time(19, 15),
        Time(19, 30)..Time(21, 0)
    )

    val defaultWeek = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
}