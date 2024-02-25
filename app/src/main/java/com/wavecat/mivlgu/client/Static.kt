package com.wavecat.mivlgu.client

import java.util.*

object Static {
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