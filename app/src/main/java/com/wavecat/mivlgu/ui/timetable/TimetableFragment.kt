package com.wavecat.mivlgu.ui.timetable

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.WeekType
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.MainViewModel
import com.wavecat.mivlgu.ui.TimetableInfo
import com.wavecat.mivlgu.ui.chat.ChatFragment


class TimetableFragment : Fragment() {

    private var _binding: TimetableFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        savedInstanceState?.getString(CACHE_KEY_ARG, null).let {
            if (it != null)
                model.restoreTimetableFromCache(it)
        }

        _binding = TimetableFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timetableName = requireArguments().getString(TIMETABLE_NAME_ARG)

        val menuProvider = object : MenuProvider {
            var info: TimetableInfo? = null

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timetable_menu, menu)

                if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext()))
                    menu.removeItem(R.id.shortcut)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.select)
                    info?.let { loadTimetableInfo(it) }

                if (menuItem.itemId == R.id.shortcut) {
                    val cacheKey = requireArguments().getString(CACHE_KEY_ARG)

                    val pinShortcutInfo =
                        ShortcutInfoCompat.Builder(requireContext(), cacheKey.toString())
                            .setIntent(
                                Intent(
                                    requireContext(),
                                    MainActivity::class.java
                                )
                                    .setAction(Intent.ACTION_VIEW)
                                    .putExtra(TIMETABLE_NAME_ARG, timetableName)
                                    .putExtra(CACHE_KEY_ARG, cacheKey)
                            )
                            .setShortLabel(timetableName.toString())
                            .setIcon(
                                IconCompat.createWithResource(
                                    requireContext(),
                                    R.drawable.ic_baseline_schedule_24
                                )
                            )
                            .build()

                    val pinnedShortcutCallbackIntent =
                        ShortcutManagerCompat.createShortcutResultIntent(requireContext(), pinShortcutInfo)

                    val successCallback = PendingIntent.getBroadcast(
                        requireContext(), 0,
                        pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    ShortcutManagerCompat.requestPinShortcut(
                        requireContext(),
                        pinShortcutInfo,
                        successCallback.intentSender
                    )
                }

                if (menuItem.itemId == R.id.chat) {
                    val navOptions = NavOptions.Builder()
                        .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                        .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

                    info?.let {
                        findNavController().navigate(
                            R.id.ChatFragment, bundleOf(
                                ChatFragment.TIMETABLE_NAME_ARG to timetableName,
                                ChatFragment.TIMETABLE_INFO_ARG to info
                            ), navOptions.build()
                        )
                    }
                }

                if (menuItem.itemId == android.R.id.home)
                    findNavController().navigateUp()

                return true
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        model.currentTimetableError.observe(viewLifecycleOwner) { error ->
            binding.error.visibility = if (error == null) View.GONE else View.VISIBLE

            if (error != null)
                binding.error.text = error.title
        }

        model.currentTimetableInfo.observe(viewLifecycleOwner) { info ->
            loadTimetableInfo(info ?: return@observe)
            menuProvider.info = info
        }

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.title = timetableName
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadTimetableInfo(info: TimetableInfo) = binding.run {
        timetable.adapter = TimetableAdapter(
            requireContext(),
            if (info.disableFilter) info.timetable else info.filteredTimetable,
            if (info.disableWeekClasses) null else info.currentWeek
        )

        even.visibility = if (info.disableFilter) View.GONE else View.VISIBLE
        odd.visibility = if (info.disableFilter) View.GONE else View.VISIBLE

        timetable.scrollToPosition(info.todayIndex)

        filter.setOnCheckedStateChangeListener(null)

        even.isChecked = info.isEven
        odd.isChecked = !info.isEven

        filter.setOnCheckedStateChangeListener { _, checkedIds ->
            val adapter = timetable.adapter ?: return@setOnCheckedStateChangeListener

            if (adapter is TimetableAdapter) {
                adapter.items = info.timetable.filter {
                    if (it is TimetableItem.ParaItem) {
                        it.para.typeWeek == WeekType.ALL ||
                                (R.id.even in checkedIds && it.para.typeWeek == WeekType.EVEN) ||
                                (R.id.odd in checkedIds && it.para.typeWeek == WeekType.ODD) ||
                                checkedIds.size == 0
                    } else
                        true
                }

                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CACHE_KEY_ARG, requireArguments().getString(CACHE_KEY_ARG))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val CACHE_KEY_ARG = "cache_key"
        const val TIMETABLE_NAME_ARG = "timetable_name"
    }
}