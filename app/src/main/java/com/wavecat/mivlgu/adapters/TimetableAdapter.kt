package com.wavecat.mivlgu.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.DayHeaderBinding
import com.wavecat.mivlgu.databinding.ItemLayoutBinding
import com.wavecat.mivlgu.databinding.KlassHeaderBinding
import com.wavecat.mivlgu.models.Para
import com.wavecat.mivlgu.models.WeekType


class TimetableAdapter(
    var items: List<TimetableItem>,
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    open class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface TimetableItem

    data class DayHeader(val title: String) : TimetableItem

    data class ParaHeader(val index: Int) : TimetableItem

    data class ParaItem(
        val para: Para,
    ) : TimetableItem

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
            is ParaHeader -> KLASS_HEADER
            is ParaItem -> ITEM_TYPE
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
        val i = items[position]

        when (holder) {
            is DayViewHolder -> {
                if (i is DayHeader) holder.binding.dayTitle.text = i.title
            }

            is KlassViewHolder -> {
                if (i is ParaHeader) {
                    val resources = holder.binding.root.context.resources

                    val time = resources.getStringArray(R.array.time)
                    val klass = resources.getStringArray(R.array.klass)

                    holder.binding.klassTime.text = time[i.index]
                    holder.binding.klassTitle.text = klass[i.index]
                }
            }

            is ItemViewHolder -> {
                if (i is ParaItem) {
                    holder.binding.itemTitle.text =
                        "${i.para.discipline}${i.para.underGroup ?: (" " + i.para.numberWeek)} ${i.para.name} ${i.para.aud}"

                    holder.binding.itemType.text = i.para.type

                    holder.binding.root.setBackgroundColor(
                        ContextCompat.getColor(
                            holder.binding.root.context, when (i.para.typeWeek) {
                                WeekType.ALL -> R.color.all
                                WeekType.EVEN -> R.color.even
                                WeekType.ODD -> R.color.odd
                            }
                        )
                    )

                    val onItemColor = ContextCompat.getColor(
                        holder.binding.root.context, when (i.para.typeWeek) {
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
    }
}

