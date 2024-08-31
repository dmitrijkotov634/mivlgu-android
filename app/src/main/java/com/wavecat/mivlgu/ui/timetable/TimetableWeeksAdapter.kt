package com.wavecat.mivlgu.ui.timetable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.WeekLayoutBinding

class TimetableWeeksAdapter(
    private val weeks: List<Int>,
    private val currentWeek: Int,
    private val listener: (Int?) -> Unit
) : RecyclerView.Adapter<TimetableWeeksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = WeekLayoutBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.week_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val week = weeks.getOrNull(position - 1)

        with(holder.binding.week) {
            text = week?.let { week.toString() } ?: "..."

            val isEven = week?.rem(2) == 0

            setBackgroundResource(
                when {
                    position == 0 -> R.drawable.all_weeks_background
                    isEven && currentWeek == week -> R.drawable.even_current_week_background
                    !isEven && currentWeek == week -> R.drawable.odd_current_week_background
                    isEven -> R.drawable.even_week_background
                    !isEven -> R.drawable.odd_week_background
                    else -> R.drawable.odd_week_background
                }
            )

            setTextColor(
                ContextCompat.getColor(
                    context,
                    when {
                        position == 0 -> R.color.on_all
                        isEven && currentWeek == week -> R.color.even
                        !isEven && currentWeek == week -> R.color.odd
                        isEven -> R.color.on_even
                        !isEven -> R.color.on_odd
                        else -> R.color.on_even
                    }
                )
            )

            setOnClickListener {
                listener(week)
            }
        }
    }

    override fun getItemCount() = weeks.size + 1
}