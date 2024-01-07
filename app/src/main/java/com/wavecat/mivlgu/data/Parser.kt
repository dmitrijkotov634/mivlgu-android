package com.wavecat.mivlgu.data

import org.jsoup.Jsoup
import java.util.*

class Parser(private val calendar: Calendar) {
    fun pickTeachers(fio: String) = Jsoup
        .connect("$BASE_URL/findteacher.php")
        .data(
            "semester", Static.getSemester(calendar),
            "year", Static.getYear(calendar),
            "fio", fio
        )
        .execute()
        .parse()
        .getElementsByTag("a").associate {
            it.attr("data-teacher-name") to it.attr("data-teacher-id").toInt()
        }

    fun pickGroups(facultyId: Int) = Jsoup
        .connect("$BASE_URL/groups.php")
        .data(
            "semester", Static.getSemester(calendar),
            "year", Static.getYear(calendar),
            "faculty", facultyId.toString(),
            "group", ""
        )
        .execute()
        .parse()
        .getElementsByTag("a").map {
            it.attr("data-group-name")
        }

    fun getWeekNumber() = Jsoup
        .connect("$BASE_URL/weekNumber.php")
        .execute()
        .parse()
        .getElementsByTag("b")
        .html()
        .toIntOrNull()

    companion object {
        private const val BASE_URL = "https://www.mivlgu.ru/out-inf/scala"
    }
}