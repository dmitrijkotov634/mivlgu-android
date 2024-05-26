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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.Constant.defaultWeek
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.models.WeekType
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.MainViewModel
import com.wavecat.mivlgu.ui.TimetableInfo
import com.wavecat.mivlgu.ui.chat.ChatFragment
import com.wavecat.mivlgu.ui.sendFeedback
import java.text.SimpleDateFormat
import java.util.*


class TimetableFragment : Fragment() {

    private var _binding: TimetableFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<MainViewModel>()

    private val repository by lazy { MainRepository(requireContext()) }

    private val args: TimetableFragmentArgs by navArgs()

    private val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

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

        val menuProvider = object : MenuProvider {
            var info: TimetableInfo? = null

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timetable_menu, menu)

                if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext()))
                    menu.removeItem(R.id.shortcut)

                if (!repository.useAnalyticsFunctions)
                    menu.removeItem(R.id.chat)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.select -> info?.let {
                        loadTimetableInfo(it)
                    }

                    R.id.shortcut -> {
                        val pinShortcutInfo =
                            ShortcutInfoCompat.Builder(requireContext(), args.cacheKey)
                                .setIntent(
                                    Intent(
                                        requireContext(),
                                        MainActivity::class.java
                                    )
                                        .setAction(Intent.ACTION_VIEW)
                                        .putExtra(TIMETABLE_NAME_ARG, args.timetableName)
                                        .putExtra(CACHE_KEY_ARG, args.cacheKey)
                                )
                                .setShortLabel(args.timetableName)
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

                    R.id.chat -> {
                        val navOptions = NavOptions.Builder()
                            .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                            .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

                        info?.let {
                            findNavController().navigate(
                                R.id.ChatFragment, bundleOf(
                                    ChatFragment.TIMETABLE_NAME_ARG to args.timetableName,
                                    ChatFragment.TIMETABLE_INFO_ARG to info
                                ), navOptions.build()
                            )
                        }
                    }

                    R.id.feedback -> sendFeedback(args.timetableName)

                    android.R.id.home -> findNavController().navigateUp()
                }

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

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.timetableName
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadTimetableInfo(info: TimetableInfo) = binding.run {
        val adapter =
            TimetableAdapter(
                requireContext(),
                info.timetable,
                if (info.disableWeekClasses) null else info.currentWeek,
                listOf()
            )

        timetable.adapter = adapter

        val filterVisibility = if (info.disableFilter) View.GONE else View.VISIBLE
        even.visibility = filterVisibility
        odd.visibility = filterVisibility

        current.visibility = if (info.disableFilter || info.currentWeek == null)
            View.GONE else View.VISIBLE

        filter.setOnCheckedStateChangeListener(null)

        if (!info.disableFilter) {
            even.isChecked = info.isEven
            odd.isChecked = !info.isEven

            adapter.setByParity(info.isEven, info.currentWeek, info.timetable, filter.checkedChipIds)
        }

        adapter.scrollToToday(timetable, info.todayIndex)

        filter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!info.disableFilter) {
                adapter.setByParity(info.isEven, info.currentWeek, info.timetable, checkedIds)
                updateDayDates(adapter, info, checkedIds)
                adapter.notifyDataSetChanged()
            }
        }

        updateDayDates(adapter, info, binding.filter.checkedChipIds)
    }

    private fun updateDayDates(adapter: TimetableAdapter, info: TimetableInfo, checkedIds: List<Int>) {
        if (info.startDate != null && info.currentWeek != null) {
            val incrementWeek = (
                    (R.id.even in checkedIds && !info.isEven) ||
                            (R.id.odd in checkedIds && info.isEven)
                    )

            adapter.setDayDates(info.startDate, info.currentWeek + if (incrementWeek) 1 else 0)
        }
    }

    private fun TimetableAdapter.setDayDates(startDate: Calendar, currentWeek: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeek)
        dates = defaultWeek.map { day ->
            calendar.set(Calendar.DAY_OF_WEEK, day)
            dateFormat.format(calendar.time)
        }
    }

    private fun TimetableAdapter.setByParity(
        isEven: Boolean,
        currentWeek: Int?,
        timetable: List<TimetableItem>,
        checkedIds: List<Int>
    ) {
        items = timetable.filter {
            if (it is TimetableItem.ParaItem) {
                if (R.id.current in checkedIds)
                    it.para.isToday(if (isEven) WeekType.EVEN else WeekType.ODD, currentWeek!!)
                else
                    it.para.typeWeek == WeekType.ALL ||
                            (R.id.even in checkedIds && it.para.typeWeek == WeekType.EVEN) ||
                            (R.id.odd in checkedIds && it.para.typeWeek == WeekType.ODD) ||
                            checkedIds.isEmpty()
            } else
                true
        }

        items = items.filterIndexed { index, item ->
            val nextItem = items.getOrNull(index + 1)

            val isHeader = (item is TimetableItem.ParaHeader)
            val nextIsHeader =
                (nextItem is TimetableItem.DayHeader || nextItem is TimetableItem.ParaHeader || nextItem == null)

            !(isHeader && nextIsHeader)
        }

        hideObviousWeekRange = R.id.current in checkedIds
    }

    private fun TimetableAdapter.scrollToToday(recyclerView: RecyclerView, day: Int) {
        items.find { it is TimetableItem.DayHeader && it.index == day }?.let {
            recyclerView.scrollToPosition(items.indexOf(it))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CACHE_KEY_ARG, args.cacheKey)
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