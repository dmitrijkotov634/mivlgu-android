package com.wavecat.mivlgu.fragment


import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.wavecat.mivlgu.adapters.MessagesAdapter
import com.wavecat.mivlgu.chat.ChatViewModel
import com.wavecat.mivlgu.databinding.ChatFragmentBinding

class ChatFragment : Fragment() {

    private var _binding: ChatFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<ChatViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ChatFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.send.setOnClickListener {
            if (binding.messageInput.editText?.text.isNullOrEmpty())
                return@setOnClickListener

            model.sendMessage(binding.messageInput.editText?.text.toString().trim())
            binding.messageInput.editText?.setText("")
        }

        binding.clear.setOnClickListener {
            model.clear()
        }

        val clipboard: ClipboardManager =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val adapter = MessagesAdapter(mutableListOf()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("text", it.content))
        }

        binding.messages.adapter = adapter

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                binding.empty.visibility =
                    if (adapter.messages.isEmpty()) View.VISIBLE else View.GONE

                super.onChanged()
            }
        })

        model.event.observe(viewLifecycleOwner) {
            adapter.messages = it.messages.drop(1)
            adapter.notifyDataSetChanged()
            binding.messages.scrollToPosition(adapter.messages.size - 1)
        }

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE

            binding.clear.isEnabled = !it
            binding.send.isEnabled = !it
            binding.messageInput.isEnabled = !it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}