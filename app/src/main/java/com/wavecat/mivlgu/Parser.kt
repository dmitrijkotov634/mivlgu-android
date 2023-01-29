package com.wavecat.mivlgu

import org.jsoup.Jsoup
import java.util.*

class Parser {
    companion object {
        private val calendar = Calendar.getInstance()

        private const val baseUrl = "https://www.mivlgu.ru/out-inf/scala"

        fun pickTeachers(fio: String): Map<String, Int> = Jsoup
            .connect("$baseUrl/findteacher.php")
            .data(
                "semester", Utils.getSemester(calendar),
                "year", Utils.getYear(calendar),
                "fio", fio
            )
            .execute()
            .parse()
            .getElementsByTag("a").associate {
                it.attr("data-teacher-name") to it.attr("data-teacher-id").toInt()
            }

        fun pickGroups(facultyId: Int): List<String> = Jsoup
            .connect("$baseUrl/groups.php")
            .data(
                "semester", Utils.getSemester(calendar),
                "year", Utils.getYear(calendar),
                "faculty", facultyId.toString(),
                "group", ""
            )
            .execute()
            .parse()
            .getElementsByTag("a").map {
                it.attr("data-group-name")
            }
    }
}