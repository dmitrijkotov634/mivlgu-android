package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wavecat.mivlgu.adapter.TimetableAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MainRepository(application)

    val currentFacultyIndex = MutableLiveData<Int>()
    val currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    val currentGroupsList: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>().also {
            selectFaculty(repository.facultyIndex)
        }
    }

    data class TimetableInfo(
        val timetable: List<TimetableAdapter.TimetableItem>,
        val filteredTimetable: List<TimetableAdapter.TimetableItem>,
        val isEven: Boolean,
        val currentDayIndex: Int
    )

    fun selectFaculty(index: Int) {
        currentFacultyIndex.postValue(index)
        viewModelScope.launch(Dispatchers.IO) {
            val data = Parser.pickGroups(facultiesIds[index])
            currentGroupsList.postValue(data)
        }
    }

    fun selectGroup(group: String) {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        var isEven = weekOfYear % 2 == 0

        viewModelScope.launch(Dispatchers.IO) {
            var dayIndex = 0

            val list = mutableListOf<TimetableAdapter.TimetableItem>()
            val filteredList = mutableListOf<TimetableAdapter.TimetableItem>()

            val data = Parser.pickTimetable(group)

            if (week.indexOf(dayOfWeek) > data.size - 1)
                isEven = !isEven

            data.forEachIndexed { index, day ->
                if (week[index] == dayOfWeek)
                    dayIndex = filteredList.size

                val dayHeader = TimetableAdapter.DayHeader(day.title)
                list.add(dayHeader)
                filteredList.add(dayHeader)

                day.klasses.forEach { klass ->
                    val klassHeader = TimetableAdapter.KlassHeader(klass.time, klass.number)
                    list.add(klassHeader)
                    filteredList.add(klassHeader)

                    klass.weeks.forEach { week ->
                        week.items.forEach {
                            val item = TimetableAdapter.KlassItem.from(week.type, it)
                            list.add(item)

                            if ((isEven && week.type == Parser.WeekType.EVEN) ||
                                (!isEven && week.type == Parser.WeekType.ODD) ||
                                week.type == Parser.WeekType.ALL
                            )
                                filteredList.add(item)
                        }
                    }
                }
            }

            val space = TimetableAdapter.DayHeader("\n")
            filteredList.add(space)
            list.add(space)

            currentTimetableInfo.postValue(
                TimetableInfo(
                    list,
                    filteredList,
                    isEven,
                    dayIndex
                )
            )
        }
    }

    fun deselect() {
        currentTimetableInfo.postValue(null)
    }

    companion object {
        val facultiesIds = listOf(2, 10, 4, 9, 16)

        val week = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
    }
}