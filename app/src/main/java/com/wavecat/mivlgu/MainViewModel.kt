package com.wavecat.mivlgu

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.wavecat.mivlgu.adapter.TimetableItem
import com.wavecat.mivlgu.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MainRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()

    private val _currentFacultyIndex = MutableLiveData<Int>()
    private val _currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    private val _currentTimetableError = MutableLiveData<TimetableError?>()
    private val _loadingException = MutableLiveData<Exception?>()

    private val _currentWeek = MutableLiveData<Int?>()

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
    val currentTimetableError: LiveData<TimetableError?> = _currentTimetableError
    val loadingException: LiveData<Exception?> = _loadingException
    val currentWeek: LiveData<Int?> = _currentWeek
    val currentGroupsList: LiveData<Pair<List<String>, List<Int>?>> = _currentGroupsList

    init {
        handleException(true) { _currentWeek.value = repository.lastWeekNumber }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val weekNumber = parser.getWeekNumber()
                _currentWeek.postValue(weekNumber)
                repository.lastWeekNumber = weekNumber
            }
        }
    }

    data class TimetableError(
        val title: String,
        val message: String
    )

    private inline fun handleException(ignore: Boolean = false, func: () -> Unit) = try {
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
            handleException {
                val data = parser.pickTeachers(fio)
                _currentGroupsList.postValue(data.keys.toList() to data.values.toList())
            }
        }
    }

    fun selectFaculty(index: Int) {
        _currentFacultyIndex.value = index

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            handleException(true) {
                _currentGroupsList.postValue(repository.getFacultyCache(index) to null)
            }

            handleException {
                val data = parser.pickGroups(Static.facultiesIds[index])
                _currentGroupsList.postValue(data to null)
                repository.saveFacultyCache(index, data)
            }

            _isLoading.postValue(false)
        }
    }

    fun selectGroup(group: String) {
        _isLoading.value = true

        handleException(true) {
            repository.getGroupsCache(group).let {
                _currentTimetableError.postValue(createTimetableError(it))
                _currentTimetableInfo.postValue(createTimetableInfo(it))
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val calendar = Calendar.getInstance()

                httpClient.scheduleGetJson(
                    group,
                    Static.getSemester(calendar),
                    Static.getYear(calendar)
                ).let {
                    _currentTimetableError.postValue(createTimetableError(it))
                    _currentTimetableInfo.postValue(createTimetableInfo(it))
                    repository.saveGroupsCache(group, it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun selectTeacher(teacherId: Int) {
        _isLoading.value = true

        handleException(true) {
            repository.getGroupsCache(teacherId.toString()).let {
                _currentTimetableError.value = createTimetableError(it)
                _currentTimetableInfo.value = createTimetableInfo(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val calendar = Calendar.getInstance()

                httpClient.scheduleGetTeacherJson(
                    teacherId,
                    Static.getSemester(calendar),
                    Static.getYear(calendar)
                ).let {
                    _currentTimetableError.postValue(createTimetableError(it))
                    _currentTimetableInfo.postValue(createTimetableInfo(it))
                    repository.saveGroupsCache(teacherId.toString(), it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun restoreTimetableFromCache(cacheKey: String) {
        _currentWeek.observeForever(object : Observer<Int?> {
            override fun onChanged(value: Int?) {
                repository.getGroupsCache(cacheKey).let {
                    _currentTimetableError.value = createTimetableError(it)
                    _currentTimetableInfo.value = createTimetableInfo(it)
                    _isLoading.value = false
                }

                _currentWeek.removeObserver(this)
            }
        })
    }

    private fun createTimetableError(data: ScheduleGetResult): TimetableError? =
        if (data.status == Status.ERROR) TimetableError(
            data.title,
            data.message
        ) else null

    private fun createTimetableInfo(data: ScheduleGetResult): TimetableInfo {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        var isEven = (_currentWeek.value ?: weekOfYear) % 2 == 0

        var dayIndex = 0

        val list = mutableListOf<TimetableItem>()
        val filteredList = mutableListOf<TimetableItem>()

        val lastDay = if (data.disciplines.size > 1) data.disciplines.keys.last().toInt() else 1
        val inverted = (Static.defaultWeek.indexOf(dayOfWeek) > lastDay - 1)

        if (inverted)
            isEven = !isEven

        data.disciplines.forEach { day ->
            val index = day.key.toInt() - 1

            val isToday = (Static.defaultWeek[index] == dayOfWeek)

            if (isToday)
                dayIndex = filteredList.size

            val dayHeader = TimetableItem.DayHeader(index)
            list.add(dayHeader)
            filteredList.add(dayHeader)

            day.value.forEach { klass ->
                val paraHeader =
                    TimetableItem.ParaHeader(klass.key.toInt() - 1, isToday)

                list.add(paraHeader)
                filteredList.add(paraHeader)

                klass.value.forEach { week ->
                    week.value.forEach {
                        val item = TimetableItem.ParaItem(it)
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