package com.wavecat.mivlgu.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.LayoutGroupBinding

class GroupAdapter(
    private val groups: List<String>,
    private val listener: (Int) -> Unit
) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = LayoutGroupBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_group, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.chip.text = groups[position]
        holder.binding.chip.setOnClickListener { listener(position) }
    }

    override fun getItemCount() = groups.size
}