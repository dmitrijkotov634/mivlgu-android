@file:Suppress("SpellCheckingInspection")

package com.wavecat.mivlgu.ui.chat.plugins

import android.app.Application
import com.wavecat.mivlgu.Constant
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.models.Para
import com.wavecat.mivlgu.ui.TimetableInfo
import com.wavecat.mivlgu.ui.chat.models.Message
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import java.util.Calendar

class Timetable(
    application: Application,
    var timetableInfo: TimetableInfo.Success? = null
) : Plugin {
    override suspend fun onPostProcessMessage(assistantMessage: Message) {}

    override suspend fun onPreProcessMessage(userMessage: Message): String {
        return buildString {
            append("Ты ассистент приложения для Муромского института")

            timetableInfo?.let {
                append("\n")
                append(it.asString())
                append("\nУчитывай подгруппу пользователя если это требуется, если подгруппа для пары не указана значит что она для двух подгрупп.")
            }
        }
    }

    private val daysNames = application.resources.getStringArray(R.array.days)
    private val time = application.resources.getStringArray(R.array.time)

    private fun TimetableInfo.Success.asString() = buildString {
        requireNotNull(currentWeek)

        append("Идёт $currentWeek учебная неделя")

        val dayOfWeek = Constant.defaultWeek.indexOf(
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        )

        timetable.forEach { item ->
            when (item) {
                is TimetableItem.DayHeader -> {
                    append("\n")
                    append(daysNames[item.index])

                    if (item.index == dayOfWeek)
                        append(" (Сегодня)")

                    append(":\n")
                }

                is TimetableItem.ParaHeader -> {
                    append(item.index + 1)
                    append(") ")
                    append("(")
                    append(time[item.index])
                    append(") ")
                }

                is TimetableItem.ParaItem -> {
                    val status = item.para.fetchTodayStatus(currentWeek)

                    when (status) {
                        Para.TodayStatus.FOR_FIRST_SUBGROUP -> append("1п/г ")
                        Para.TodayStatus.FOR_SECOND_SUBGROUP -> append("2п/г ")
                        Para.TodayStatus.FOR_ALL -> {}
                        Para.TodayStatus.NOT_TODAY -> return@forEach
                    }

                    append(item.para.type.uppercase())
                    append(" ")
                    append(item.para.discipline)
                    append(" ")
                    append(item.para.name)
                    append(" ")
                    append(item.para.audience)

                    if (item.para.groupName.isNotBlank()) {
                        append(" ")
                        append(item.para.groupName)
                    }

                    append("\n")
                }

                is TimetableItem.Warning -> {}
            }
        }
    }

    override suspend fun onClearContext() {}
}