package com.wavecat.mivlgu.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.wavecat.mivlgu.MainViewModel
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapter.GroupAdapter
import com.wavecat.mivlgu.databinding.GroupFragmentBinding

class GroupFragment : Fragment() {

    private var _binding: GroupFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = GroupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentFacultyIndex.observe(viewLifecycleOwner) {
            binding.chipGroup.check(binding.chipGroup.getChildAt(it).id)
        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            val index = group.indexOfChild(
                group.findViewById<Chip>(group.checkedChipId)
            )

            model.selectFaculty(index)
            model.repository.facultyIndex = index

            binding.progressBar.visibility = View.VISIBLE
        }

        model.currentGroupsList.observe(viewLifecycleOwner) { group ->
            binding.groups.adapter = GroupAdapter(group) {
                model.selectGroup(group[it])

                val builder = NavOptions.Builder()
                    .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                    .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

                findNavController().navigate(R.id.TimetableFragment, null, builder.build())
            }

            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}