package com.wavecat.mivlgu.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.chat.models.Message
import com.wavecat.mivlgu.databinding.ChatMessageBinding

class MessagesAdapter(
    var messages: List<Message>,
    val onClick: (Message) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ChatMessageBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_message, parent, false)
        )
    }

    @SuppressLint("RtlHardcoded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.role.text = messages[position].role.replaceFirstChar {
            it.uppercase()
        }

        holder.binding.content.text = messages[position].content
        holder.binding.content.setOnClickListener { onClick(messages[position]) }
    }

    override fun getItemCount() = messages.size
}