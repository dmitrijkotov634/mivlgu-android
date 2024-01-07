package com.wavecat.mivlgu.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.wavecat.mivlgu.MainViewModel
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.TimetableAdapter
import com.wavecat.mivlgu.adapter.TimetableItem
import com.wavecat.mivlgu.data.TimetableInfo
import com.wavecat.mivlgu.data.WeekType
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding


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

        val menuProvider = object : MenuProvider {
            var info: TimetableInfo? = null

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timetable_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.select)
                    info?.let { loadTimetableInfo(it) }

                if (menuItem.itemId == R.id.chat) {
                    val builder = NavOptions.Builder()
                        .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                        .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

                    info?.let {
                        findNavController().navigate(
                            R.id.ChatFragment, bundleOf(
                                ChatFragment.TIMETABLE_NAME_ARG to requireArguments().getString(TIMETABLE_NAME_ARG),
                                ChatFragment.TIMETABLE_INFO_ARG to info
                            ), builder.build()
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

        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            requireArguments().getString(TIMETABLE_NAME_ARG)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadTimetableInfo(info: TimetableInfo) = binding.run {
        timetable.adapter = TimetableAdapter(requireContext(), info.filteredTimetable, info.currentWeek)

        timetable.scrollToPosition(info.currentDayIndex)

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