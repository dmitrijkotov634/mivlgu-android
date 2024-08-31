package com.wavecat.mivlgu.ui.timetable

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wavecat.mivlgu.Constant.defaultWeek
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.models.WeekType
import com.wavecat.mivlgu.databinding.SelectWeekDialogBinding
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.MainViewModel
import com.wavecat.mivlgu.ui.TimetableInfo
import com.wavecat.mivlgu.ui.chat.ChatFragment
import com.wavecat.mivlgu.ui.sendFeedback
import com.wavecat.mivlgu.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class TimetableFragment : Fragment() {

    private var _binding: TimetableFragmentBinding? = null
    private val binding get() = _binding!!
    private val model by activityViewModels<MainViewModel>()
    private val settingsModel by activityViewModels<SettingsViewModel>()
    private val repository by lazy { MainRepository(requireContext()) }
    private val args: TimetableFragmentArgs by navArgs()
    private val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        reloadFromBundle(savedInstanceState)

        _binding = TimetableFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun reloadFromBundle(savedInstanceState: Bundle?, fromCache: Boolean = false) {
        savedInstanceState?.getString(GROUP_ARG, null)?.let {
            if (fromCache)
                return@let model.restoreTimetableFromCache(it)

            if (it.isNotEmpty())
                model.selectGroup(it)
        }

        savedInstanceState?.getString(TEACHER_ID_ARG, null)?.let {
            if (fromCache)
                return@let model.restoreTimetableFromCache(it)

            if (it.isNotEmpty())
                model.selectTeacher(it.toInt())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuProvider = object : MenuProvider {
            var info: TimetableInfo.Success? = null

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timetable_menu, menu)

                if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext()))
                    menu.removeItem(R.id.shortcut)

                if (!repository.allowAssistant || repository.disableAI)
                    menu.removeItem(R.id.chat)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.select -> info?.let {
                        loadTimetableInfo(it)
                    }

                    R.id.shortcut -> {
                        val pinShortcutInfo =
                            ShortcutInfoCompat.Builder(requireContext(), args.timetableName)
                                .setIntent(
                                    Intent(
                                        requireContext(),
                                        MainActivity::class.java
                                    )
                                        .setAction(Intent.ACTION_VIEW)
                                        .putExtra(TIMETABLE_NAME_ARG, args.timetableName)
                                        .putExtra(TEACHER_ID_ARG, args.teacherId)
                                        .putExtra(GROUP_ARG, args.group)
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
                            ShortcutManagerCompat.createShortcutResultIntent(
                                requireContext(),
                                pinShortcutInfo
                            )

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

                        if (info?.currentWeek == null) return true

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

        model.currentTimetableInfo.observe(viewLifecycleOwner) { info ->
            when (info) {
                is TimetableInfo.Failure -> {
                    binding.error.visibility = View.VISIBLE
                    binding.error.text = info.title
                }

                is TimetableInfo.Success -> {
                    binding.error.visibility = View.GONE
                    loadTimetableInfo(info)
                    menuProvider.info = info
                }

                else -> {}
            }
        }

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        settingsModel.showWeekParityFilter.observe(viewLifecycleOwner) {
            binding.suggestWeekParityFilter.setOnCheckedChangeListener(null)
            binding.suggestWeekParityFilter.isChecked = it
            binding.suggestWeekParityFilter.setOnCheckedChangeListener { _, isChecked ->
                settingsModel.changeShowParityFilter(isChecked)
                reloadFromBundle(args.toBundle(), fromCache = true)
            }
        }

        settingsModel.showWeekChooser.observe(viewLifecycleOwner) {
            binding.suggestWeekChooser.setOnCheckedChangeListener(null)
            binding.suggestWeekChooser.isChecked = it
            binding.suggestWeekChooser.setOnCheckedChangeListener { _, isChecked ->
                settingsModel.changeShowWeekChooser(isChecked)
                reloadFromBundle(args.toBundle(), fromCache = true)
            }
        }

        binding.suggestOk.setOnClickListener {
            repository.suggestionVersion = 1
            makeTransition()
            binding.suggestion.visibility = View.GONE
        }

        binding.suggestion.visibility =
            if (repository.suggestionVersion < 1) View.VISIBLE else View.GONE

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.timetableName
    }

    private fun makeTransition() {
        val transition = AutoTransition()
        transition.excludeChildren(binding.timetable, true)
        transition.excludeTarget(binding.timetable, true)
        TransitionManager.beginDelayedTransition(binding.root, transition)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadTimetableInfo(info: TimetableInfo.Success) = binding.run {
        val adapter =
            TimetableAdapter(
                requireContext(),
                info.timetable,
                if (info.disableWeekClasses) null else info.currentWeek,
                listOf(),
                info.showDatesAndCurrentKlassHints
            )

        timetable.adapter = adapter

        val filterVisibility = if (info.showWeekParityFilter) View.VISIBLE else View.GONE

        even.visibility = filterVisibility
        odd.visibility = filterVisibility

        current.visibility = if (info.showCurrentWeek) View.VISIBLE else View.GONE

        if (info.showCurrentWeek) {
            binding.filter.isSelectionRequired = true
            current.isChecked = true
            current.setText(R.string.current)
        }

        if (info.showWeekParityFilter && !info.showCurrentWeek) {
            even.isChecked = info.isEven
            odd.isChecked = !info.isEven
        }

        val oneOfFiltersVisible = info.showWeekParityFilter || info.showCurrentWeek

        if (oneOfFiltersVisible)
            adapter.setByParity(
                info.currentWeek,
                info.timetable,
                filter.checkedChipIds
            )

        current.setOnClickListener {
            if (R.id.current !in binding.filter.checkedChipIds)
                return@setOnClickListener

            val dialogBinding = SelectWeekDialogBinding.inflate(layoutInflater)

            val dialog = BottomSheetDialog(requireContext()).apply {
                setContentView(dialogBinding.root)
                show()
            }

            dialogBinding.weeks.adapter =
                TimetableWeeksAdapter(
                    List(info.maxWeekNumber) { it + 1 },
                    info.currentWeek!!
                ) { selectedWeek ->
                    adapter.apply {
                        if (selectedWeek == null) {
                            binding.filter.isSelectionRequired = false
                            current.isChecked = false
                            return@apply
                        }

                        currentWeek = selectedWeek
                        setByParity(selectedWeek, info.timetable, binding.filter.checkedChipIds)

                        if (info.showDatesAndCurrentKlassHints)
                            updateDayDates(this, info, binding.filter.checkedChipIds)

                        if (selectedWeek == info.currentWeek)
                            current.setText(R.string.current)
                        else
                            current.text = getString(R.string.week, selectedWeek)

                        notifyDataSetChanged()
                    }

                    dialog.cancel()
                }
        }

        filter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filterByWeek = R.id.current in binding.filter.checkedChipIds

            binding.filter.isSelectionRequired = filterByWeek

            if (!filterByWeek) {
                current.setText(R.string.current)
                adapter.currentWeek = info.currentWeek
            }

            if (oneOfFiltersVisible) {
                adapter.setByParity(info.currentWeek, info.timetable, checkedIds)

                if (info.showDatesAndCurrentKlassHints)
                    updateDayDates(adapter, info, checkedIds)

                adapter.notifyDataSetChanged()
            }
        }

        if (info.showDatesAndCurrentKlassHints)
            updateDayDates(adapter, info, binding.filter.checkedChipIds)

        adapter.scrollToToday(timetable, info.todayIndex)
    }

    private fun updateDayDates(
        adapter: TimetableAdapter,
        info: TimetableInfo.Success,
        checkedIds: List<Int>
    ) {
        info.startDate?.let { startDate ->
            info.currentWeek?.let { currentWeek ->
                val isCurrentWeek = currentWeek == adapter.currentWeek
                val shouldIncrementWeek = (R.id.even in checkedIds && !info.isEven) ||
                        (R.id.odd in checkedIds && info.isEven)

                val weekToSet = if (isCurrentWeek) {
                    currentWeek + if (shouldIncrementWeek) 1 else 0
                } else {
                    adapter.currentWeek ?: currentWeek
                }

                adapter.setDayDates(startDate, weekToSet)
            }
        }
    }

    private fun TimetableAdapter.setDayDates(startDate: Calendar, weekNumber: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, weekNumber)
        dates = defaultWeek.map { day ->
            calendar.set(Calendar.DAY_OF_WEEK, day)
            dateFormat.format(calendar.time)
        }
    }

    private fun TimetableAdapter.setByParity(
        currentWeek: Int?,
        timetable: List<TimetableItem>,
        checkedIds: List<Int>
    ) {
        items = timetable.filter {
            if (it is TimetableItem.ParaItem) {
                if (R.id.current in checkedIds) {
                    it.para.isLessonToday(currentWeek!!)
                } else
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

        outState.putString(TEACHER_ID_ARG, args.teacherId)
        outState.putString(GROUP_ARG, args.group)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TEACHER_ID_ARG = "teacher_id"
        const val GROUP_ARG = "group"

        const val CACHE_KEY_ARG = "cache_key"
        const val TIMETABLE_NAME_ARG = "timetable_name"
    }
}