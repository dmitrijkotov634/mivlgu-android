package com.wavecat.mivlgu.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.Parser
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.DayHeaderBinding
import com.wavecat.mivlgu.databinding.ItemLayoutBinding
import com.wavecat.mivlgu.databinding.KlassHeaderBinding


class TimetableAdapter(
    var items: List<TimetableItem>,
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    open class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface TimetableItem

    data class DayHeader(val title: String) : TimetableItem

    data class KlassHeader(val time: String, val title: String) : TimetableItem

    data class KlassItem(
        val weekType: Parser.WeekType,
        val type: String,
        val text: String,
    ) : TimetableItem {
        companion object {
            fun from(weekType: Parser.WeekType, item: Parser.Item): KlassItem {
                return KlassItem(
                    weekType,
                    item.type,
                    "${item.disciplineName} ${item.numberWeek}${item.underGroup} ${item.name} ${item.aud}"
                )
            }
        }
    }

    class DayViewHolder(view: View) : ViewHolder(view) {
        var binding = DayHeaderBinding.bind(view)
    }

    class KlassViewHolder(view: View) : ViewHolder(view) {
        var binding = KlassHeaderBinding.bind(view)
    }

    class ItemViewHolder(view: View) : ViewHolder(view) {
        var binding = ItemLayoutBinding.bind(view)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is DayHeader -> DAY_HEADER
            is KlassHeader -> KLASS_HEADER
            is KlassItem -> ITEM_TYPE
            else -> throw IllegalArgumentException()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is DayViewHolder -> {
                if (item is DayHeader) holder.binding.dayTitle.text = item.title
            }

            is KlassViewHolder -> {
                if (item is KlassHeader) {
                    holder.binding.klassTime.text = item.time.replace("-", " -")
                    holder.binding.klassTitle.text = item.title
                }
            }

            is ItemViewHolder -> {
                if (item is KlassItem) {
                    holder.binding.itemTitle.text = item.text
                    holder.binding.itemType.text = item.type

                    holder.binding.root.setBackgroundColor(
                        ContextCompat.getColor(
                            holder.binding.root.context, when (item.weekType) {
                                Parser.WeekType.ALL -> R.color.all
                                Parser.WeekType.EVEN -> R.color.even
                                Parser.WeekType.ODD -> R.color.odd
                            }
                        )
                    )

                    val onItemColor = ContextCompat.getColor(
                        holder.binding.root.context, when (item.weekType) {
                            Parser.WeekType.ALL -> R.color.on_all
                            Parser.WeekType.EVEN -> R.color.on_even
                            Parser.WeekType.ODD -> R.color.on_odd
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
    }
}

