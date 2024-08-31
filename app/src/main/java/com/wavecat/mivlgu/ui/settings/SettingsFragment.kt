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
import androidx.transition.TransitionManager
import com.wavecat.mivlgu.databinding.SettingsFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.donate.BillingViewModel
import com.wavecat.mivlgu.ui.donate.DonateDialog
import com.wavecat.mivlgu.ui.donate.ThanksDialog
import com.wavecat.mivlgu.ui.timetable.TimetableFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private var _binding: SettingsFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<SettingsViewModel>()
    private val billingModel by activityViewModels<BillingViewModel>()

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

        model.showWeekParityFilter.observe(viewLifecycleOwner) {
            binding.showParityFilter.setOnCheckedChangeListener(null)
            binding.showParityFilter.isChecked = it
            binding.showParityFilter.setOnCheckedChangeListener { _, isChecked ->
                model.changeShowParityFilter(isChecked)
            }
        }

        model.disableWeekClasses.observe(viewLifecycleOwner) {
            binding.disableWeekClasses.setOnCheckedChangeListener(null)
            binding.disableWeekClasses.isChecked = it
            binding.showWeekChooser.isEnabled = !it
            binding.disableWeekClasses.setOnCheckedChangeListener { _, isChecked ->
                model.changeDisableWeekClasses(isChecked)
            }
        }

        model.showWeekChooser.observe(viewLifecycleOwner) {
            binding.showWeekChooser.setOnCheckedChangeListener(null)
            binding.showWeekChooser.isChecked = it
            binding.showWeekChooser.setOnCheckedChangeListener { _, isChecked ->
                model.changeShowWeekChooser(isChecked)
            }
        }

        model.disableIEP.observe(viewLifecycleOwner) {
            binding.disableIep.setOnCheckedChangeListener(null)
            binding.disableIep.isChecked = it
            binding.disableIep.setOnCheckedChangeListener { _, isChecked ->
                model.changeDisableIEP(isChecked)
                (requireActivity() as MainActivity).invalidateNavMenu()
                onNavMenuChanged()
            }
        }

        model.disableAI.observe(viewLifecycleOwner) {
            binding.disableAi.setOnCheckedChangeListener(null)
            binding.disableAi.isChecked = it
            binding.disableAi.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) openUrl(TERMS_OF_USE)
                model.changeDisableAI(isChecked)
                (requireActivity() as MainActivity).invalidateNavMenu()
                onNavMenuChanged()
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

        model.showRouteTime.observe(viewLifecycleOwner) {
            binding.showRouteTime.setOnCheckedChangeListener(null)
            binding.showRouteTime.isChecked = it
            binding.showRouteTime.setOnCheckedChangeListener { _, isChecked ->
                model.changeShowRouteTime(isChecked)
            }
        }

        model.showExperiments.observe(viewLifecycleOwner) {
            binding.run {
                val visibility = if (it) View.VISIBLE else View.GONE
                title2.visibility = visibility
                analysisInfo.visibility = visibility
                showPrevGroup.visibility = visibility
                showTeacherPath.visibility = visibility
                showRouteTime.visibility = visibility
                preload.visibility = visibility
            }
        }

        binding.vk.setOnClickListener { openUrl(VK) }

        binding.about.setOnClickListener(object : View.OnClickListener {
            var clicks = 0

            override fun onClick(v: View?) {
                if (clicks++ < 8) return

                model.generateEasterEgg()
                model.showExperiments()

                (requireActivity() as MainActivity).enableNotifications()

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

        binding.coffee.setOnClickListener {
            DonateDialog().show(childFragmentManager, "DonateDialog")
        }

        billingModel.billingMade.observe(viewLifecycleOwner) { status ->
            if (status) {
                billingModel.buyMore()
                ThanksDialog().show(childFragmentManager, "ThanksDialog")
            }
        }

        billingModel.billingAvailability.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.coffee.visibility = if (it) View.VISIBLE else View.GONE
        }

        billingModel.checkAvailability()
    }

    private fun onNavMenuChanged() = binding.run {
        disableAi.isEnabled = false
        disableIep.isEnabled = false

        lifecycleScope.launch {
            delay(500)

            disableAi.isEnabled = true
            disableIep.isEnabled = true
        }
    }

    private fun openUrl(url: String) =
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TERMS_OF_USE = "https://telegra.ph/mivlgu-08-30"
        const val VK = "https://vk.com/bomb3r"
    }
}