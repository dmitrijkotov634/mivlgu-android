package com.wavecat.mivlgu.ui.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.Constant
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.models.Para
import com.wavecat.mivlgu.client.models.WeekEnumeration
import com.wavecat.mivlgu.client.models.WeekRange
import com.wavecat.mivlgu.client.models.WeekType
import com.wavecat.mivlgu.databinding.DayHeaderBinding
import com.wavecat.mivlgu.databinding.ItemLayoutBinding
import com.wavecat.mivlgu.databinding.KlassHeaderBinding
import com.wavecat.mivlgu.databinding.WarningCardBinding
import java.util.Calendar


class TimetableAdapter(
    private val context: Context,
    var items: List<TimetableItem>,
    private val currentWeek: Int?,
    var dates: List<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var hideObviousWeekRange = false

    private val subgroup = context.getString(R.string.subgroup)

    private val daysNames = context.resources.getStringArray(R.array.days)
    private val time = context.resources.getStringArray(R.array.time)
    private val klass = context.resources.getStringArray(R.array.klass)

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = DayHeaderBinding.bind(view)
    }

    class KlassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = KlassHeaderBinding.bind(view)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ItemLayoutBinding.bind(view)
    }

    class WarningViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = WarningCardBinding.bind(view)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is TimetableItem.DayHeader -> DAY_HEADER
            is TimetableItem.ParaHeader -> KLASS_HEADER
            is TimetableItem.ParaItem -> PARA_ITEM
            is TimetableItem.Warning -> WARNING_CARD
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

            PARA_ITEM -> ItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_layout, parent, false)
            )

            WARNING_CARD -> WarningViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.warning_card, parent, false)
            )

            else -> throw IllegalArgumentException()
        }

    private fun WeekEnumeration.toPrettyHtmlString(
        weekType: WeekType,
        groupNumber: Int? = null
    ): String =
        enumeration.joinToString(",") { range ->
            when (range) {
                is WeekRange.WeekParityRange -> buildString {
                    if (currentWeek != null && range.isLessonToday(weekType, currentWeek)) {
                        append(FORMAT_START)
                        if (groupNumber != null) append("<font color=\"${groupColors[groupNumber]}\">")
                        append(range.start)
                        append("-")
                        append(range.end)
                        if (groupNumber != null) append("</font>")
                        append(FORMAT_END)
                    } else {
                        append(range.start)
                        append("-")
                        append(range.end)
                    }
                }

                is WeekRange.Week -> if (range.weekNumber == currentWeek)
                    buildString {
                        append(FORMAT_START)
                        if (groupNumber != null) append("<font color=\"${groupColors[groupNumber]}\">")
                        append(range.weekNumber.toString())
                        if (groupNumber != null) append("")
                        append(FORMAT_END)
                    }
                else
                    range.weekNumber.toString()

                is WeekRange.InvalidRange -> "<font color=red>invalid:'${range.unparsed}'</font>"
            }
        }

    private fun buildGroupWeekEnumString(para: Para, enumeration: WeekEnumeration, groupId: Int) =
        "${groupId + 1}$subgroup ${enumeration.toPrettyHtmlString(para.typeWeek, groupId)}"

    private fun buildWeeksString(para: Para) =
        if (para.subGroup.isNullOrEmpty()) {
            if (hideObviousWeekRange) "" else para.parsedWeekNumber.toPrettyHtmlString(para.typeWeek)
        } else if (hideObviousWeekRange) {
            val groupId = when (para.fetchTodayStatus(currentWeek!!)) {
                Para.TodayStatus.FOR_FIRST_SUBGROUP -> 0
                Para.TodayStatus.FOR_SECOND_SUBGROUP -> 1
                else -> -1
            }

            "<font color=\"${groupColors[groupId]}\">${groupId + 1}$subgroup</font>"
        } else {
            buildList {
                if (!para.subGroup1.isNullOrEmpty())
                    add(buildGroupWeekEnumString(para, para.parsedSubGroup1, 0))

                if (!para.subGroup2.isNullOrEmpty())
                    add(buildGroupWeekEnumString(para, para.parsedSubGroup2, 1))
            }.joinToString(" ")
        }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is DayViewHolder -> {
                if (item !is TimetableItem.DayHeader) return

                holder.binding.dayTitle.text = daysNames.getOrNull(item.index)
                holder.binding.date.text = dates.getOrNull(item.index)
            }

            is KlassViewHolder -> {
                if (item !is TimetableItem.ParaHeader) return

                holder.binding.klassTime.text = time.getOrNull(item.index)
                holder.binding.klassTitle.text = klass.getOrNull(item.index)

                val current = Calendar.getInstance().apply {
                    val temp = Calendar.getInstance()
                    timeInMillis = 0
                    set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
                }

                Constant.timeRanges.getOrNull(item.index)?.also { timeRange ->
                    holder.binding.klassStatus.visibility =
                        if (item.isToday && timeRange.check(current)) View.VISIBLE else View.GONE
                }
            }

            is WarningViewHolder -> {
                if (item !is TimetableItem.Warning) return

                holder.binding.warning.setText(
                    when (item) {
                        TimetableItem.Warning.CURRENT_WEEK_NULL -> R.string.warning_current_week
                        TimetableItem.Warning.HAS_INVALID_RANGES -> R.string.warning_has_invalid_ranges
                    }
                )
            }

            is ItemViewHolder -> {
                if (item !is TimetableItem.ParaItem) return

                val para = item.para

                holder.binding.itemTitle.text =
                    HtmlCompat.fromHtml(
                        "${para.discipline} ${buildWeeksString(para)} ${para.name} ${para.audience} ${para.groupName}",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )

                holder.binding.itemType.text = para.type.uppercase()

                holder.binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        context, when (para.typeWeek) {
                            WeekType.ALL -> R.color.all
                            WeekType.EVEN -> R.color.even
                            WeekType.ODD -> R.color.odd
                        }
                    )
                )

                val onItemColor = ContextCompat.getColor(
                    context, when (para.typeWeek) {
                        WeekType.ALL -> R.color.on_all
                        WeekType.EVEN -> R.color.on_even
                        WeekType.ODD -> R.color.on_odd
                    }
                )

                holder.binding.itemType.setTextColor(onItemColor)
                holder.binding.itemTitle.setTextColor(onItemColor)

                holder.binding.itemTitle.setTextIsSelectable(false)
                holder.binding.itemTitle.post { holder.binding.itemTitle.setTextIsSelectable(true) }

                item.para.extraData.run {
                    holder.binding.extraData.visibility = View.GONE
                    holder.binding.extraData1.visibility = View.GONE
                    holder.binding.extraData2.visibility = View.GONE

                    if (this != null) {
                        if (prevName != null) {
                            holder.binding.extraData.text = "↶ $prevName ($prevGroupName)"
                            holder.binding.extraData.visibility = View.VISIBLE
                        }

                        if (prevBuilding != null) {
                            holder.binding.extraData1.text =
                                "$prevBuilding ↝ ${item.para.audience.split("/").last()}"
                            holder.binding.extraData1.visibility = View.VISIBLE
                        }

                        if (routeTime != null) {
                            holder.binding.extraData2.text = "↝ $routeTime m."
                            holder.binding.extraData2.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    companion object {
        const val DAY_HEADER = 1
        const val KLASS_HEADER = 2
        const val PARA_ITEM = 3
        const val WARNING_CARD = 4

        const val FORMAT_START = "<b><big>"
        const val FORMAT_END = "</big></b>"

        val groupColors = listOf("#1E90FF", "#FF4500")
    }
}

