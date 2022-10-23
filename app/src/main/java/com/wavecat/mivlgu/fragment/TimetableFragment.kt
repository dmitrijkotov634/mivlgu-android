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
import com.wavecat.mivlgu.adapters.TimetableAdapter
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding
import com.wavecat.mivlgu.models.WeekType

class TimetableFragment : Fragment() {

    private var _binding: TimetableFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = TimetableFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentTimetableInfo.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe

            binding.timetable.adapter = TimetableAdapter(info.filteredTimetable)

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

            binding.progressBar.visibility = View.INVISIBLE
        }

        model.loadingException.observe(viewLifecycleOwner) {
            binding.error1.text = if (it == null) "" else it.message
            binding.error1.visibility = if (it == null) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.deselect()
        _binding = null
    }
}