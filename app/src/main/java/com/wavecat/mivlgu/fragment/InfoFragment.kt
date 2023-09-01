package com.wavecat.mivlgu.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.wavecat.mivlgu.databinding.InfoFragmentBinding


class InfoFragment : Fragment() {

    private var _binding: InfoFragmentBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = InfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.telegram.setOnClickListener { openUrl(TELEGRAM) }
        binding.github.setOnClickListener { openUrl(GITHUB) }
        binding.vk.setOnClickListener { openUrl(VK) }

        binding.iep.setOnClickListener { openUrl(IEP) }

        binding.iep.setOnLongClickListener {
            openUrl(IEP_2012)
            true
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TELEGRAM = "https://t.me/wavecat"
        const val GITHUB = "https://github.com/dmitrijkotov634/mivlgu-android"
        const val VK = "https://vk.com/bomb3r"

        const val IEP = "https://www.mivlgu.ru/iop/"
        const val IEP_2012 = "https://www.mivlgu.ru/iop2012/"
    }
}