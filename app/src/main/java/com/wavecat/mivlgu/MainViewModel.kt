package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wavecat.mivlgu.adapters.TimetableAdapter
import com.wavecat.mivlgu.models.WeekType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MainRepository(application)

    val currentFacultyIndex = MutableLiveData<Int>()
    val currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    val loadingException = MutableLiveData<Exception?>()

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

    private fun pickGroups(facultyId: Int): List<String> {
        val calendar = Calendar.getInstance()
        return Jsoup
            .connect("https://www.mivlgu.ru/out-inf/scala/groups.php")
            .data(
                "semester", "1",
                "year", calendar.get(Calendar.YEAR).toString(),
                "faculty", facultyId.toString(),
                "group", ""
            )
            .execute()
            .parse()
            .getElementsByTag("a").map {
                it.attr("data-group-name")
            }
    }

    fun selectFaculty(index: Int) {
        currentFacultyIndex.value = index
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = pickGroups(facultiesIds[index])
                currentGroupsList.postValue(data)
            } catch (e: Exception) {
                loadingException.postValue(e)
            }
        }
    }

    fun selectGroup(group: String, names: Array<String>) {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        var isEven = weekOfYear % 2 == 0

        viewModelScope.launch(Dispatchers.IO) {
            var dayIndex = 0

            val list = mutableListOf<TimetableAdapter.TimetableItem>()
            val filteredList = mutableListOf<TimetableAdapter.TimetableItem>()

            try {
                val data =
                    Client().scheduleGetJson(group, "1", calendar.get(Calendar.YEAR).toString())

                if (week.indexOf(dayOfWeek) > data.disciplines.size - 1)
                    isEven = !isEven

                data.disciplines.forEach { day ->
                    val index = day.key.toInt() - 1

                    if (week[index] == dayOfWeek)
                        dayIndex = filteredList.size

                    val dayHeader = TimetableAdapter.DayHeader(names[index])
                    list.add(dayHeader)
                    filteredList.add(dayHeader)

                    day.value.forEach { klass ->
                        val paraHeader =
                            TimetableAdapter.ParaHeader(klass.key.toInt() - 1)

                        list.add(paraHeader)
                        filteredList.add(paraHeader)

                        klass.value.forEach { week ->
                            week.value.forEach {
                                val item = TimetableAdapter.ParaItem(it)
                                list.add(item)
                                if ((isEven && it.typeWeek == WeekType.EVEN) ||
                                    (!isEven && it.typeWeek == WeekType.ODD) ||
                                    it.typeWeek == WeekType.ALL
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

            } catch (e: Exception) {
                loadingException.postValue(e)
                return@launch
            }
        }
    }

    fun deselect() {
        currentTimetableInfo.value = null
    }

    fun closeErrorDialog() {
        loadingException.value = null
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