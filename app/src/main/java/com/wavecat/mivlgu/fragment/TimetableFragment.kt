package com.wavecat.mivlgu.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wavecat.mivlgu.MainViewModel
import com.wavecat.mivlgu.Parser
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.TimetableAdapter
import com.wavecat.mivlgu.databinding.TimetableFragmentBinding

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

        model.loadingError.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            LoadingErrorDialog().apply {
                val bundle = Bundle()
                bundle.putString(LoadingErrorDialog.EXCEPTION_ARG, it.message)
                arguments = bundle
            }.show(
                childFragmentManager, LoadingErrorDialog.TAG
            )
            model.closeErrorDialog()
        }

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
                        if (it is TimetableAdapter.KlassItem) {
                            it.weekType == Parser.WeekType.ALL ||
                                    (R.id.even in checkedIds && it.weekType == Parser.WeekType.EVEN) ||
                                    (R.id.odd in checkedIds && it.weekType == Parser.WeekType.ODD) ||
                                    checkedIds.size == 0
                        } else
                            true
                    }

                    adapter.notifyDataSetChanged()
                }
            }

            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.deselect()
        _binding = null
    }
}