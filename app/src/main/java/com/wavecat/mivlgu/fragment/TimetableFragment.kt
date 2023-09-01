package com.wavecat.mivlgu.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wavecat.mivlgu.MainViewModel
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.TimetableAdapter
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
        savedInstanceState?.getString(CACHE_KEY, null).let {
            if (it != null)
                model.restoreTimetableFromCache(it)
        }

        _binding = TimetableFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentTimetableInfo.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe

            binding.timetable.adapter = TimetableAdapter(requireContext(), info.filteredTimetable, info.currentWeek)

            binding.timetable.scrollToPosition(info.currentDayIndex)

            binding.filter.setOnCheckedStateChangeListener(null)

            binding.even.isChecked = info.isEven
            binding.odd.isChecked = !info.isEven

            binding.filter.setOnCheckedStateChangeListener { _, checkedIds ->
                val adapter = binding.timetable.adapter ?: return@setOnCheckedStateChangeListener
                if (adapter is TimetableAdapter) {
                    adapter.items = info.timetable.filter {
                        if (it is TimetableAdapter.ParaItem) {
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

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CACHE_KEY, requireArguments().getString(CACHE_KEY))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val CACHE_KEY = "cache_key"
    }
}