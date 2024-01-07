@file:Suppress("SpellCheckingInspection")

package com.wavecat.mivlgu.ai.plugins

import android.app.Application
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.TimetableItem
import com.wavecat.mivlgu.ai.models.Message
import com.wavecat.mivlgu.data.Static
import com.wavecat.mivlgu.data.TimetableInfo
import com.wavecat.mivlgu.data.WeekType
import java.util.*

class Timetable(
    application: Application,
    var timetableInfo: TimetableInfo? = null
) : Plugin {
    override suspend fun onPostProcessMessage(assistantMessage: Message) {}

    override suspend fun onPreProcessMessage(userMessage: Message): String {
        return buildString {
            append("Ты ассистент приложения для Муромского института")

            timetableInfo?.let {
                append("\n")
                append(it.asString())
                append("\n")
                append("Учитывай подгруппу пользователя если это требуется, расписание составлено для обоих подгрупп")
            }
        }
    }

    private val daysNames = application.resources.getStringArray(R.array.days)
    private val time = application.resources.getStringArray(R.array.time)

    private fun TimetableInfo.asString() = buildString {
        currentWeek?.let {
            append("Идёт $it учебная неделя")
        }

        val dayOfWeek = Static.defaultWeek.indexOf(
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        )

        timetable.forEach { item ->
            when (item) {
                is TimetableItem.DayHeader -> {
                    append("\n")
                    append(daysNames[item.title])

                    if (item.title == dayOfWeek)
                        append(" (Сегодня)")

                    append(":\n")
                }

                is TimetableItem.ParaHeader -> {
                    append(item.index + 1)
                    append(" пара ")
                    append(time[item.index])
                    append(":\n")
                }

                is TimetableItem.ParaItem -> {
                    append("- ")
                    append(item.para.discipline)
                    append(" (")
                    append(item.para.aud)
                    append(") - ")
                    append(item.para.type)
                    append(" - ")
                    append(item.para.name)
                    append(" - ")

                    if (item.para.groupName.isNotBlank()) {
                        append(item.para.groupName)
                        append(" - ")
                    }

                    if (item.para.underGroup.isNullOrEmpty()) {
                        append("Недели для каждой подгруппы: ")
                        append(item.para.numberWeek)
                        append(
                            when (item.para.typeWeek) {
                                WeekType.EVEN -> " только чётные"
                                WeekType.ODD -> " только нечётные"
                                else -> ""
                            }
                        )
                    } else
                        append(
                            buildList {
                                if (!item.para.underGroup1.isNullOrEmpty())
                                    add("Недели для 1 подгруппы: ${item.para.underGroup1}")

                                if (!item.para.underGroup2.isNullOrEmpty())
                                    add("Недели для 2 подгруппы: ${item.para.underGroup2}")
                            }.joinToString("; ")
                        )

                    append("\n")
                }
            }
        }
    }

    override suspend fun onClearContext() {}
}