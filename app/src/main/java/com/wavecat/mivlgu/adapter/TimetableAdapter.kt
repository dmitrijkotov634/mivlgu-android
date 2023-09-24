package com.wavecat.mivlgu.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.data.Para
import com.wavecat.mivlgu.data.WeekType
import com.wavecat.mivlgu.databinding.DayHeaderBinding
import com.wavecat.mivlgu.databinding.ItemLayoutBinding
import com.wavecat.mivlgu.databinding.KlassHeaderBinding
import java.util.*


class TimetableAdapter(
    val context: Context,
    var items: List<TimetableItem>,
    private val currentWeek: Int?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val subgroup = context.getString(R.string.subgroup)

    private val daysNames: Array<String> = context.resources.getStringArray(R.array.days)

    private val time: Array<String> = context.resources.getStringArray(R.array.time)
    private val klass: Array<String> = context.resources.getStringArray(R.array.klass)

    interface TimetableItem

    data class DayHeader(val title: Int) : TimetableItem

    data class ParaHeader(val index: Int, val isToday: Boolean) : TimetableItem

    data class ParaItem(
        val para: Para,
    ) : TimetableItem

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = DayHeaderBinding.bind(view)
    }

    class KlassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = KlassHeaderBinding.bind(view)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ItemLayoutBinding.bind(view)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is DayHeader -> DAY_HEADER
            is ParaHeader -> KLASS_HEADER
            is ParaItem -> ITEM_TYPE
            else -> throw IllegalArgumentException()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            DAY_HEADER -> DayViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.day_header, parent, false)
            )

            KLASS_HEADER -> KlassViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.klass_header, parent, false)
            )

            ITEM_TYPE -> ItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_layout, parent, false)
            )

            else -> throw IllegalArgumentException()
        }

    private fun formatRange(numberWeek: String, weekType: WeekType): String {
        if (currentWeek == null)
            return numberWeek

        val parts = numberWeek
            .split("-")
            .map { it.toInt() }

        val weekStart = parts[0]
        val weekEnd = parts[1]

        val isEven = currentWeek % 2 == 0

        return if (currentWeek in weekStart..weekEnd &&
            (weekType == WeekType.ALL ||
                    (isEven && weekType == WeekType.EVEN) ||
                    (!isEven && weekType == WeekType.ODD))
        )
            "$FORMAT_START$numberWeek$FORMAT_END"
        else
            numberWeek
    }

    private fun formatWeeks(
        underGroup: String,
        weekType: WeekType,
        group: Int? = null
    ): String =
        underGroup
            .split(",")
            .joinToString(",") {
                it.let { week ->
                    if (week.contains("-")) {
                        formatRange(week, weekType)
                    } else {
                        val weekNumber = week.toInt()
                        if (weekNumber == currentWeek)
                            buildString {
                                append(FORMAT_START)

                                if (group != null)
                                    append("<font color=\"${groupColors[group]}\">")

                                append(week)

                                if (group != null)
                                    append("</font>")

                                append(FORMAT_END)
                            }
                        else
                            week
                    }
                }
            }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val i = items[position]

        when (holder) {
            is DayViewHolder -> {
                if (i is DayHeader)
                    holder.binding.dayTitle.text = if (i.title == -1) "\n" else daysNames[i.title]
            }

            is KlassViewHolder -> {
                if (i is ParaHeader) {
                    holder.binding.klassTime.text = time[i.index]
                    holder.binding.klassTitle.text = klass[i.index]

                    val current = Calendar.getInstance().apply {
                        val temp = Calendar.getInstance()
                        timeInMillis = 0
                        set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
                    }

                    holder.binding.klassStatus.visibility =
                        if (i.isToday && timeRanges[i.index].check(current)) View.VISIBLE else View.GONE
                }
            }

            is ItemViewHolder -> {
                if (i is ParaItem) {
                    val underGroup = if (i.para.underGroup.isNullOrEmpty()) {
                        formatWeeks(i.para.numberWeek, i.para.typeWeek)
                    } else {
                        buildList {
                            if (!i.para.underGroup1.isNullOrEmpty())
                                add("1$subgroup ${formatWeeks(i.para.underGroup1, i.para.typeWeek, 0)}")

                            if (!i.para.underGroup2.isNullOrEmpty())
                                add("2$subgroup ${formatWeeks(i.para.underGroup2, i.para.typeWeek, 1)}")
                        }.joinToString(" ")
                    }

                    holder.binding.itemTitle.text =
                        HtmlCompat.fromHtml(
                            "${i.para.discipline} $underGroup ${i.para.name} ${i.para.aud} ${i.para.groupName}",
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        )

                    holder.binding.itemType.text = i.para.type

                    holder.binding.root.setBackgroundColor(
                        ContextCompat.getColor(
                            context, when (i.para.typeWeek) {
                                WeekType.ALL -> R.color.all
                                WeekType.EVEN -> R.color.even
                                WeekType.ODD -> R.color.odd
                            }
                        )
                    )

                    val onItemColor = ContextCompat.getColor(
                        context, when (i.para.typeWeek) {
                            WeekType.ALL -> R.color.on_all
                            WeekType.EVEN -> R.color.on_even
                            WeekType.ODD -> R.color.on_odd
                        }
                    )

                    holder.binding.itemType.setTextColor(onItemColor)
                    holder.binding.itemTitle.setTextColor(onItemColor)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    companion object {
        const val DAY_HEADER = 1
        const val KLASS_HEADER = 2
        const val ITEM_TYPE = 3

        const val FORMAT_START = "<b><big>"
        const val FORMAT_END = "</big></b>"

        val groupColors = listOf("#1E90FF", "#FF4500")

        data class Time(
            val hour: Int,
            val minute: Int
        ) {
            operator fun rangeTo(time: Time) = TimeRange(this, time)

            fun toCalendar(): Calendar = Calendar.getInstance().apply {
                timeInMillis = 0
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
        }

        data class TimeRange(
            val start: Time,
            val end: Time
        ) {
            fun check(current: Calendar): Boolean =
                current.timeInMillis >= start.toCalendar().timeInMillis
                        && current.timeInMillis <= end.toCalendar().timeInMillis
        }

        val timeRanges: List<TimeRange> = listOf(
            Time(8, 30)..Time(10, 0),
            Time(10, 15)..Time(11, 45),
            Time(12, 30)..Time(14, 0),
            Time(14, 15)..Time(15, 45),
            Time(16, 0)..Time(17, 30),
            Time(17, 45)..Time(19, 15),
            Time(19, 30)..Time(21, 0)
        )
    }
}

