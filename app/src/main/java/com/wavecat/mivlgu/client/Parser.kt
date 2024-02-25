package com.wavecat.mivlgu.client

import org.jsoup.Jsoup

class Parser {
    fun pickTeachers(fio: String) = Jsoup
        .connect("$BASE_URL/findteacher.php")
        .data(
            "fio", fio,
            "semester", "",
            "year", ""
        )
        .execute()
        .parse()
        .getElementsByTag("a").associate {
            it.attr("data-teacher-name") to it.attr("data-teacher-id").toInt()
        }

    fun pickGroups(facultyId: Int, semester: String, year: String) = Jsoup
        .connect("$BASE_URL/groups.php")
        .data(
            "faculty", facultyId.toString(),
            "semester", semester,
            "year", year,
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