package com.wavecat.mivlgu.client.models

class InvalidRangeException(message: String) : Exception(message)

sealed interface WeekRange {
    fun isToday(weekType: WeekType, currentWeek: Int): Boolean

    data class InvalidRange(val unparsed: String) : WeekRange {
        override fun isToday(weekType: WeekType, currentWeek: Int): Boolean {
            throw InvalidRangeException("Trying to operate with InvalidRange: '$unparsed'")
        }
    }

    data class Week(val week: Int) : WeekRange {
        override fun isToday(weekType: WeekType, currentWeek: Int): Boolean =
            week == currentWeek
    }

    data class WeekParityRange(val start: Int, val end: Int) : WeekRange {
        override fun isToday(weekType: WeekType, currentWeek: Int): Boolean =
            (currentWeek in start..end &&
                    (weekType == WeekType.ALL ||
                            (currentWeek % 2 == 0 && weekType == WeekType.EVEN) ||
                            (currentWeek % 2 != 0 && weekType == WeekType.ODD))
                    )
    }
}

data class WeekEnumeration(val enumeration: List<WeekRange>) {
    fun isToday(weekType: WeekType, currentWeek: Int): Boolean {
        for (range in enumeration)
            if (range.isToday(weekType, currentWeek))
                return true
        return false
    }
}