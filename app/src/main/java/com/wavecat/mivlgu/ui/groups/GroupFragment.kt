package com.wavecat.mivlgu.ui.groups

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.GroupFragmentBinding
import com.wavecat.mivlgu.ui.MainActivity
import com.wavecat.mivlgu.ui.MainViewModel
import com.wavecat.mivlgu.ui.sendFeedback
import com.wavecat.mivlgu.ui.timetable.TimetableFragment


class GroupFragment : Fragment() {

    private var _binding: GroupFragmentBinding? = null

    private val binding get() = _binding!!

    private val model by activityViewModels<MainViewModel>()

    private val repository by lazy { MainRepository(requireContext()) }

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

        val navOptions = NavOptions.Builder()
            .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
            .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (_binding == null) return

                val index = _binding!!.chipGroup.indexOfChild(
                    binding.chipGroup.findViewById<Chip>(binding.chipGroup.checkedChipId)
                )

                menuInflater.inflate(R.menu.main_search_menu, menu)

                if (index != MainViewModel.TEACHER_INDEX) {
                    menu.removeItem(R.id.app_bar_search)
                    return
                }

                val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView

                searchView.apply {
                    maxWidth = Integer.MAX_VALUE
                    setIconifiedByDefault(true)

                    isIconified = false

                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            model.findTeacher(query.toString())
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean = true
                    })

                    queryHint = getString(R.string.search_hint)
                    clearFocus()
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    android.R.id.home -> findNavController().navigateUp()
                    R.id.settings -> findNavController().navigate(
                        R.id.SettingsFragment,
                        null,
                        navOptions.build()
                    )

                    R.id.feedback -> sendFeedback(getString(R.string.app_name))
                }

                return true
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            requireActivity().invalidateMenu()

            val index = group.indexOfChild(group.findViewById<Chip>(group.checkedChipId))

            repository.facultyIndex = index

            if (index == MainViewModel.TEACHER_INDEX)
                model.findTeacher(repository.teacherFio)
            else
                model.selectFaculty(index)
        }

        model.currentGroupsList.observe(viewLifecycleOwner) { group ->
            binding.groups.layoutManager =
                GridLayoutManager(context, if (group.second == null) 2 else 1)

            binding.searchIcon.visibility =
                if (group.first.isEmpty() && group.second != null) View.VISIBLE else View.GONE

            binding.groups.adapter = GroupAdapter(group.first) {
                requireActivity().invalidateMenu()

                val name = group.second?.let { teacherList ->
                    model.selectTeacher(teacherList[it])
                    group.first[it]
                } ?: run {
                    model.selectGroup(group.first[it])
                    group.first[it]
                }

                findNavController().navigate(
                    GroupFragmentDirections.actionGroupFragmentToTimetableFragment(
                        timetableName = name,
                        group = group.second?.let { null } ?: group.first[it],
                        teacherId = group.second?.get(it)?.toString()
                    )
                )

                val imm: InputMethodManager? =
                    getSystemService(requireContext(), InputMethodManager::class.java)

                imm?.hideSoftInputFromWindow(view.windowToken, 0)

                val shortcutInfo =
                    ShortcutInfoCompat.Builder(requireContext(), name)
                        .setIntent(
                            Intent(
                                requireContext(),
                                MainActivity::class.java
                            )
                                .setAction(Intent.ACTION_VIEW)
                                .putExtra(TimetableFragment.TIMETABLE_NAME_ARG, name)
                                .putExtra(
                                    TimetableFragment.TEACHER_ID_ARG,
                                    group.second?.get(it)?.toString()
                                )
                                .putExtra(
                                    TimetableFragment.GROUP_ARG,
                                    group.second?.let { null } ?: group.first[it])
                        )
                        .setShortLabel(name)
                        .setIcon(
                            IconCompat.createWithResource(
                                requireContext(),
                                R.drawable.ic_baseline_schedule_24
                            )
                        )
                        .build()

                ShortcutManagerCompat.pushDynamicShortcut(requireContext(), shortcutInfo)
            }
        }

        model.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}