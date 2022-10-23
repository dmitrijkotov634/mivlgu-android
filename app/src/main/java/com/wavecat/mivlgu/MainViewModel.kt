package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wavecat.mivlgu.adapters.TimetableAdapter
import com.wavecat.mivlgu.models.ScheduleGetResult
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

    val currentGroupsList: MutableLiveData<Pair<List<String>, List<Int>?>> by lazy {
        MutableLiveData<Pair<List<String>, List<Int>?>>().also {
            if (repository.facultyIndex != teacherIndex)
                selectFaculty(repository.facultyIndex)
            else if (!repository.teacherFio.isNullOrEmpty())
                selectTeacher(repository.teacherFio!!)
        }
    }

    data class TimetableInfo(
        val timetable: List<TimetableAdapter.TimetableItem>,
        val filteredTimetable: List<TimetableAdapter.TimetableItem>,
        val isEven: Boolean,
        val currentDayIndex: Int
    )

    private fun pickTeachers(fio: String): Map<String, Int> {
        val calendar = Calendar.getInstance()
        return Jsoup
            .connect(baseUrl + "findteacher.php")
            .data(
                "semester", "1",
                "year", calendar.get(Calendar.YEAR).toString(),
                "fio", fio
            )
            .execute()
            .parse()
            .getElementsByTag("a").associate {
                it.attr("data-teacher-name") to it.attr("data-teacher-id").toInt()
            }
    }

    private fun pickGroups(facultyId: Int): List<String> {
        val calendar = Calendar.getInstance()
        return Jsoup
            .connect(baseUrl + "groups.php")
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

    private suspend fun runAndCatch(func: suspend () -> Unit) = try {
        func()
        loadingException.postValue(null)
    } catch (e: Exception) {
        loadingException.postValue(e)
    }

    fun selectTeacher(fio: String?) {
        currentFacultyIndex.value = teacherIndex

        if (fio.isNullOrEmpty()) {
            currentGroupsList.value = listOf<String>() to listOf()
            return
        }

        repository.teacherFio = fio

        viewModelScope.launch(Dispatchers.IO) {
            runAndCatch {
                val data = pickTeachers(fio)
                currentGroupsList.postValue(data.keys.toList() to data.values.toList())
            }
        }
    }

    fun selectFaculty(index: Int) {
        currentFacultyIndex.value = index
        viewModelScope.launch(Dispatchers.IO) {
            currentGroupsList.postValue(repository.getFacultyCache(index) to null)
            runAndCatch {
                val data = pickGroups(facultiesIds[index])
                currentGroupsList.postValue(data to null)
                repository.saveFacultyCache(index, data)
            }
        }
    }

    fun selectGroup(group: String, names: Array<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            runAndCatch {
                next(repository.getGroupsCache(group), names)
                val result = client.scheduleGetJson(
                    group,
                    "1",
                    Calendar.getInstance().get(Calendar.YEAR).toString()
                )
                next(result, names)
                repository.saveGroupsCache(group, result)
            }
        }

    fun selectTeacher(teacherId: Int, names: Array<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            runAndCatch {
                next(repository.getGroupsCache(teacherId.toString()), names)
                val result = client.scheduleGetTeacherJson(
                    teacherId,
                    "1",
                    Calendar.getInstance().get(Calendar.YEAR).toString()
                )
                next(result, names)
                repository.saveGroupsCache(teacherId.toString(), result)
            }
        }

    private fun next(data: ScheduleGetResult, names: Array<String>) {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        var isEven = weekOfYear % 2 == 0

        var dayIndex = 0

        val list = mutableListOf<TimetableAdapter.TimetableItem>()
        val filteredList = mutableListOf<TimetableAdapter.TimetableItem>()

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
    }

    fun deselect() {
        currentTimetableInfo.value = null
    }

    companion object {
        const val baseUrl = "https://www.mivlgu.ru/out-inf/scala/"
        const val teacherIndex = 5

        val client = Client()

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