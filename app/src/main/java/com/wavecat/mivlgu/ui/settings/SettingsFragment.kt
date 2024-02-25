package com.wavecat.mivlgu.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.wavecat.mivlgu.databinding.SettingsFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.timetable.TimetableFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private var _binding: SettingsFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.preload.setOnClickListener {
            model.preload()
            binding.preload.isEnabled = false
        }

        model.disableFilter.observe(viewLifecycleOwner) {
            binding.disableFilter.setOnCheckedChangeListener(null)
            binding.disableFilter.isChecked = it
            binding.disableFilter.setOnCheckedChangeListener { _, isChecked ->
                model.changeDisableFilter(isChecked)
            }
        }

        model.disableWeekClasses.observe(viewLifecycleOwner) {
            binding.disableWeekClasses.setOnCheckedChangeListener(null)
            binding.disableWeekClasses.isChecked = it
            binding.disableWeekClasses.setOnCheckedChangeListener { _, isChecked ->
                model.changeDisableWeekClasses(isChecked)
            }
        }

        model.disableIEP.observe(viewLifecycleOwner) {
            binding.disableIep.setOnCheckedChangeListener(null)
            binding.disableIep.isChecked = it
            binding.disableIep.setOnCheckedChangeListener { _, isChecked ->
                model.changeDisableIEP(isChecked)
                (requireActivity() as MainActivity).invalidateNavMenu()
                binding.disableIep.isEnabled = false
                lifecycleScope.launch {
                    delay(500)
                    binding.disableIep.isEnabled = true
                }
            }
        }

        model.showTeacherPath.observe(viewLifecycleOwner) {
            binding.showTeacherPath.setOnCheckedChangeListener(null)
            binding.showTeacherPath.isChecked = it
            binding.showTeacherPath.setOnCheckedChangeListener { _, isChecked ->
                model.changeShowTeacherPath(isChecked)
            }
        }

        model.showPrevGroups.observe(viewLifecycleOwner) {
            binding.showPrevGroup.setOnCheckedChangeListener(null)
            binding.showPrevGroup.isChecked = it
            binding.showPrevGroup.setOnCheckedChangeListener { _, isChecked ->
                model.changeShowPrevGroups(isChecked)
            }
        }

        binding.github.setOnClickListener { openUrl(GITHUB) }
        binding.vk.setOnClickListener { openUrl(VK) }

        binding.about.setOnClickListener(object : View.OnClickListener {
            var clicks = 0
            override fun onClick(v: View?) {
                if (clicks++ < 4) return
                model.generateEasterEgg()
                startActivity(
                    Intent(
                        requireContext(),
                        MainActivity::class.java
                    )
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(TimetableFragment.TIMETABLE_NAME_ARG, EasterEgg.NAME)
                        .putExtra(TimetableFragment.CACHE_KEY_ARG, SettingsViewModel.EASTER_EGG_KEY)
                )
            }
        })
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val GITHUB = "https://github.com/dmitrijkotov634/mivlgu-android"
        const val VK = "https://vk.com/bomb3r"
    }
}