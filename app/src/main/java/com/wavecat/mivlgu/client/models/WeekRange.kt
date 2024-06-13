package com.wavecat.mivlgu.client.models

class InvalidRangeException(message: String) : Exception(message)

sealed interface WeekRange {
    fun isLessonToday(weekType: WeekType, targetWeekNumber: Int): Boolean

    data class InvalidRange(val unparsed: String) : WeekRange {
        override fun isLessonToday(weekType: WeekType, targetWeekNumber: Int): Boolean {
            throw InvalidRangeException("Trying to operate with InvalidRange: '$unparsed'")
        }
    }

    data class Week(val weekNumber: Int) : WeekRange {
        override fun isLessonToday(weekType: WeekType, targetWeekNumber: Int): Boolean =
            weekNumber == targetWeekNumber
    }

    data class WeekParityRange(val start: Int, val end: Int) : WeekRange {
        override fun isLessonToday(weekType: WeekType, targetWeekNumber: Int): Boolean =
            (targetWeekNumber in start..end &&
                    (weekType == WeekType.ALL ||
                            (targetWeekNumber % 2 == 0 && weekType == WeekType.EVEN) ||
                            (targetWeekNumber % 2 != 0 && weekType == WeekType.ODD))
                    )
    }
}

data class WeekEnumeration(val enumeration: List<WeekRange>) {
    fun isLessonToday(weekType: WeekType, weekNumber: Int): Boolean {
        for (range in enumeration)
            if (range.isLessonToday(weekType, weekNumber))
                return true
        return false
    }

    fun isEmpty() = enumeration.isEmpty()

    companion object {
        val EMPTY = WeekEnumeration(emptyList())
    }
}