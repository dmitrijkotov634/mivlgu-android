@file:Suppress("SpellCheckingInspection")

package com.wavecat.mivlgu.ui.settings

import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.models.Para
import com.wavecat.mivlgu.client.models.Status
import com.wavecat.mivlgu.client.models.WeekType

object EasterEgg {

    const val NAME = "Дмитрий К."

    private val disciplines = listOf(
        "Сидеть за ПК",
        "Усердно сидеть за ПК",
        "Задумчиво сидеть за ПК",
        "Работать за ПК",
        "Чаепитие",
        "Ничего не делать",
        "Сон",
        "Спать",
        "Задумчиво спать",
        "Мечтательно спать",
        "Печально спать",
        "Перерыв",
        "Выйти на улицу"
    )

    private val types = listOf("Лб", "Пр", "Лк")

    private val weekLayouts = listOf(
        "1,2-25,25-52",
        "0,1-52",
        "0,1-5,6,7-52",
        "0-52",
        "1,2,3,4-51,52",
        "1,0-52",
        "0,1-51,52"
    )

    private fun generateClass(
        day: Int,
        klass: Int,
        weekType: WeekType,
        forGroups: Boolean = false
    ) = Para(
        idDay = day,
        numberPara = klass,
        discipline = disciplines.random(),
        type = types.random(),
        typeWeek = weekType,
        audience = "Дома",
        weekNumber = if (forGroups) "" else weekLayouts.random(),
        comment = "Без комментариев.",
        zaoch = null,
        name = NAME,
        groupName = "",
        countFieldsLabs = null,
        subGroup = if (forGroups) "empty" else null,
        subGroup1 = if (forGroups) weekLayouts.random() else null,
        subGroup2 = if (forGroups) weekLayouts.random() else null,
        extraData = null
    )

    private fun generateClasses(day: Int, klass: Int, forGroups: Boolean = false) = mapOf(
        "odd" to listOf(generateClass(day, klass, WeekType.ODD, forGroups)),
        "even" to listOf(generateClass(day, klass, WeekType.EVEN, forGroups))
    )

    private fun generateDay(day: Int) = buildMap {
        repeat(6) { klass ->
            val index = klass + 1
            put(index.toString(), generateClasses(day, index))
        }

        put("7", generateClasses(day, 7, true))
    }

    fun generate() = ScheduleGetResult(
        status = Status.OK,
        time = "",
        group = null,
        teacher = null,
        title = "",
        message = "",
        semester = "",
        year = "",
        disciplines = buildMap {
            repeat(7) { day ->
                val index = day + 1
                put(index.toString(), generateDay(index))
            }
        })
}