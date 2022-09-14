package com.wavecat.mivlgu.fragment

import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.wavecat.mivlgu.MainViewModel
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.adapters.GroupAdapter
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
            requireActivity().invalidateMenu()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

                if (_binding == null) return

                val index = _binding!!.chipGroup.indexOfChild(
                    binding.chipGroup.findViewById<Chip>(binding.chipGroup.checkedChipId)
                )

                if (index == MainViewModel.teacherIndex) {
                    menuInflater.inflate(R.menu.main_search_menu, menu)

                    val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView

                    searchView.setIconifiedByDefault(true)
                    searchView.isIconified = false

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            model.selectTeacher(query.toString())
                            binding.progressBar.visibility = View.VISIBLE
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean = true
                    })
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home)
                    findNavController().navigateUp()
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            requireActivity().invalidateMenu()

            val index = group.indexOfChild(
                group.findViewById<Chip>(group.checkedChipId)
            )

            model.repository.facultyIndex = index

            binding.progressBar.visibility = View.VISIBLE

            if (index == MainViewModel.teacherIndex)
                model.selectTeacher(model.repository.teacherFio)
            else model.selectFaculty(index)
        }

        model.currentGroupsList.observe(viewLifecycleOwner) { group ->

            binding.groups.layoutManager =
                GridLayoutManager(context, if (group.second == null) 2 else 1)

            binding.groups.adapter = GroupAdapter(group.first) {
                requireActivity().invalidateMenu()

                if (group.second == null) model.selectGroup(
                    group.first[it],
                    resources.getStringArray(R.array.days)
                )
                else model.selectTeacher(group.second!![it], resources.getStringArray(R.array.days))

                val builder = NavOptions.Builder()
                    .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                    .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

                findNavController().navigate(R.id.TimetableFragment, null, builder.build())

                val imm: InputMethodManager? =
                    getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }

            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}