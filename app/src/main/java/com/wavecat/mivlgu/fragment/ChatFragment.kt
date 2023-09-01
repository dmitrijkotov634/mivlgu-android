package com.wavecat.mivlgu.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.MessagesAdapter
import com.wavecat.mivlgu.chat.ChatViewModel
import com.wavecat.mivlgu.databinding.ChatFragmentBinding
import com.wavecat.mivlgu.databinding.MessageEditBinding
import java.util.*

class ChatFragment : Fragment(), RecognitionListener {

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

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(this)

        binding.send.setOnClickListener {
            if (binding.messageInput.editText?.text.isNullOrEmpty()) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(android.Manifest.permission.RECORD_AUDIO),
                        1
                    )
                }

                val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                speechRecognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                speechRecognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )
                speechRecognizer.startListening(speechRecognizerIntent)
            } else {
                model.sendMessage(binding.messageInput.editText?.text.toString().trim())
                binding.messageInput.editText?.setText("")
            }
        }

        binding.clear.setOnClickListener { model.clear() }

        val adapter = MessagesAdapter(requireContext(), mutableListOf()) { position, message ->
            val messageEdit = MessageEditBinding.inflate(layoutInflater)
            messageEdit.text.setText(message.content)

            MaterialAlertDialogBuilder(requireContext())
                .setView(messageEdit.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    message.content = messageEdit.text.text.toString()
                    binding.messages.adapter?.notifyItemChanged(position)
                }
                .setNegativeButton(R.string.delete) { _, _ ->
                    model.deleteMessage(position)
                }
                .show()
        }

        binding.messages.adapter = adapter

        model.event.observe(viewLifecycleOwner) {
            adapter.messages = it.messages

            adapter.apply {
                if (it.isNewMessage) {
                    notifyItemInserted(messages.size - 1)
                } else {
                    notifyDataSetChanged()
                }

                if (messages.isEmpty())
                    return@apply

                binding.messages.scrollToPosition(messages.size - 1)
            }

            binding.empty.visibility =
                if (it.messages.isEmpty()) View.VISIBLE else View.GONE
        }

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE

            binding.clear.isEnabled = !it
            binding.send.isEnabled = !it
            binding.messageInput.isEnabled = !it
        }

        model.messageInputText.observe(viewLifecycleOwner) {
            binding.messageInput.editText?.setText(it)
            model.messageInputText.removeObservers(viewLifecycleOwner)
        }

        model.noInternetConnection.observe(viewLifecycleOwner) {
            if (it) Snackbar.make(binding.root, R.string.no_internet, Snackbar.LENGTH_SHORT).show()
        }

        binding.messageInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                model.saveInputText(text.toString())

                binding.send.setImageResource(
                    if (text.isNullOrEmpty())
                        R.drawable.baseline_keyboard_voice_24 else R.drawable.baseline_send_24
                )
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {}

    override fun onResults(results: Bundle?) {
        val data = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        data?.get(0)?.let { model.sendMessage(it.trim()) }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}