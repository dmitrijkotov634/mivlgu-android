package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.wavecat.mivlgu.adapter.TimetableAdapter
import com.wavecat.mivlgu.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MainRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()

    private val _currentFacultyIndex = MutableLiveData<Int>()
    private val _currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    private val _loadingException = MutableLiveData<Exception?>()

    private val _currentWeek = MutableLiveData<Int>()

    private val _currentGroupsList: MutableLiveData<Pair<List<String>, List<Int>?>> by lazy {
        MutableLiveData<Pair<List<String>, List<Int>?>>().also {
            if (repository.facultyIndex != TEACHER_INDEX)
                selectFaculty(repository.facultyIndex)
            else if (!repository.teacherFio.isNullOrEmpty())
                selectTeacher(repository.teacherFio!!)
        }
    }

    val isLoading: LiveData<Boolean> = _isLoading
    val currentFacultyIndex: LiveData<Int> = _currentFacultyIndex
    val currentTimetableInfo: LiveData<TimetableInfo?> = _currentTimetableInfo
    val loadingException: LiveData<Exception?> = _loadingException
    val currentWeek: LiveData<Int> = _currentWeek
    val currentGroupsList: LiveData<Pair<List<String>, List<Int>?>> = _currentGroupsList

    init {
        viewModelScope.launch(Dispatchers.IO) {
            tryCatch { _currentWeek.postValue(parser.getWeekNumber()) }
        }
    }

    data class TimetableInfo(
        val timetable: List<TimetableAdapter.TimetableItem>,
        val filteredTimetable: List<TimetableAdapter.TimetableItem>,
        val isEven: Boolean,
        val currentDayIndex: Int,
        val currentWeek: Int?
    )

    private inline fun tryCatch(ignore: Boolean = false, func: () -> Unit) = try {
        func()
        _loadingException.postValue(null)
    } catch (e: Exception) {
        if (!ignore)
            _loadingException.postValue(e)

        e.printStackTrace()
    }

    fun selectTeacher(fio: String?) {
        _currentFacultyIndex.value = TEACHER_INDEX

        if (fio.isNullOrEmpty()) {
            _currentGroupsList.value = listOf<String>() to listOf()
            return
        }

        repository.teacherFio = fio

        viewModelScope.launch(Dispatchers.IO) {
            tryCatch {
                val data = parser.pickTeachers(fio)
                _currentGroupsList.postValue(data.keys.toList() to data.values.toList())
            }
        }
    }

    fun selectFaculty(index: Int) {
        _currentFacultyIndex.value = index

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            tryCatch(true) {
                _currentGroupsList.postValue(repository.getFacultyCache(index) to null)
            }

            tryCatch {
                val data = parser.pickGroups(Static.facultiesIds[index])
                _currentGroupsList.postValue(data to null)
                repository.saveFacultyCache(index, data)
            }

            _isLoading.postValue(false)
        }
    }

    fun selectGroup(group: String) =
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            tryCatch(true) {
                _currentTimetableInfo.postValue(createTimetableInfo(repository.getGroupsCache(group)))
            }

            tryCatch {
                val calendar = Calendar.getInstance()
                val result = httpClient.scheduleGetJson(
                    group,
                    Static.getSemester(calendar),
                    Static.getYear(calendar)
                )
                _currentTimetableInfo.postValue(createTimetableInfo(result))
                repository.saveGroupsCache(group, result)
            }

            _isLoading.postValue(false)
        }

    fun selectTeacher(teacherId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            tryCatch(true) {
                _currentTimetableInfo.postValue(createTimetableInfo(repository.getGroupsCache(teacherId.toString())))
            }

            tryCatch {
                val calendar = Calendar.getInstance()
                val result = httpClient.scheduleGetTeacherJson(
                    teacherId,
                    Static.getSemester(calendar),
                    Static.getYear(calendar)
                )
                _currentTimetableInfo.postValue(createTimetableInfo(result))
                repository.saveGroupsCache(teacherId.toString(), result)
            }

            _isLoading.postValue(false)
        }

    fun restoreTimetableFromCache(cacheKey: String) {
        _currentWeek.observeForever(object : Observer<Int> {
            override fun onChanged(value: Int) {
                _currentTimetableInfo.value = createTimetableInfo(repository.getGroupsCache(cacheKey))
                _isLoading.value = false
                _currentWeek.removeObserver(this)
            }
        })
    }

    private fun createTimetableInfo(data: ScheduleGetResult): TimetableInfo {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        var isEven = (_currentWeek.value ?: weekOfYear) % 2 == 0

        var dayIndex = 0

        val list = mutableListOf<TimetableAdapter.TimetableItem>()
        val filteredList = mutableListOf<TimetableAdapter.TimetableItem>()

        val inverted = (Static.defaultWeek.indexOf(dayOfWeek) > data.disciplines.size - 1).apply {
            if (this) isEven = !isEven
        }

        data.disciplines.forEach { day ->
            val index = day.key.toInt() - 1

            val isToday = (Static.defaultWeek[index] == dayOfWeek).apply {
                if (this) dayIndex = filteredList.size
            }

            val dayHeader = TimetableAdapter.DayHeader(index)
            list.add(dayHeader)
            filteredList.add(dayHeader)

            day.value.forEach { klass ->
                val paraHeader =
                    TimetableAdapter.ParaHeader(klass.key.toInt() - 1, isToday)

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

        val space = TimetableAdapter.DayHeader(-1)
        filteredList.add(space)
        list.add(space)

        return TimetableInfo(
            list,
            filteredList,
            isEven,
            dayIndex,
            _currentWeek.value?.plus(if (inverted) 1 else 0)
        )
    }

    companion object {
        const val TEACHER_INDEX = 5

        val parser = Parser(Calendar.getInstance())
        val httpClient = HttpClient()
    }
}