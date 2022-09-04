package com.wavecat.mivlgu

import org.jsoup.Jsoup
import java.util.*

object Parser {
    private const val baseUrl = "https://www.mivlgu.ru/out-inf/scala/"

    fun pickGroups(facultyId: Int): List<String> {
        val calendar = Calendar.getInstance()
        return Jsoup
            .connect(baseUrl + "groups.php")
            .data(
                "semester", "1",
                "year", calendar.get(Calendar.YEAR).toString(),
                "faculty", facultyId.toString(),
                "group", ""
            )
            .execute()
            .parse()
            .getElementsByTag("a").map {
                it.attr("data-group-name")
            }
    }

    fun pickTimetable(groupName: String): List<Day> {
        val calendar = Calendar.getInstance()
        return Jsoup
            .connect(baseUrl + "sch_group.php")
            .data(
                "semester", "1",
                "year", calendar.get(Calendar.YEAR).toString(),
                "faculty", "",
                "group", groupName
            )
            .execute()
            .parse()
            .getElementsByClass("day").map { day ->
                Day(day.getElementsByTag("h3").text(), day.getElementsByClass("para").map { klass ->
                    Klass(
                        klass.getElementsByClass("numb").text(),
                        klass.getElementsByClass("time").text(),
                        klass.getElementsByClass("week").map { week ->
                            Week(
                                WeekType.valueOf(week.attr("class").split("-")[1].uppercase()),
                                week.getElementsByClass("item").map { item ->
                                    Item(
                                        item.getElementsByClass("discipline-name").text(),
                                        item.getElementsByClass("type").text(),
                                        item.getElementsByClass("number_week").text(),
                                        item.getElementsByClass("name").text(),
                                        item.getElementsByClass("aud").text(),
                                        item.getElementsByClass("under_group").text(),
                                        item.getElementsByClass("group_name").text()
                                    )
                                })
                        })
                })
            }
    }


    data class Day(
        val title: String,
        val klasses: List<Klass>
    )

    data class Klass(
        val number: String,
        val time: String,
        val weeks: List<Week>
    )

    data class Week(
        val type: WeekType,
        val items: List<Item>
    )

    enum class WeekType {
        ALL, ODD, EVEN
    }

    data class Item(
        val disciplineName: String,
        val type: String,
        val numberWeek: String,
        val name: String,
        val aud: String,
        val underGroup: String,
        val groupName: String
    )
}