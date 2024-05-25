package com.wavecat.mivlgu.ui

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wavecat.mivlgu.Constant
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.client.HttpClient
import com.wavecat.mivlgu.client.Parser
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.models.Status
import com.wavecat.mivlgu.ui.settings.SettingsViewModel
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import com.wavecat.mivlgu.workers.BuildModelWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MainRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentFacultyIndex = MutableLiveData<Int>()
    val currentFacultyIndex: LiveData<Int> = _currentFacultyIndex

    private val _currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    val currentTimetableInfo: LiveData<TimetableInfo?> = _currentTimetableInfo

    private val _currentTimetableError = MutableLiveData<TimetableError?>()
    val currentTimetableError: LiveData<TimetableError?> = _currentTimetableError

    private val _loadingException = MutableLiveData<Exception?>()
    val loadingException: LiveData<Exception?> = _loadingException

    private val _currentWeek = MutableLiveData<Int?>()
    val currentWeek: LiveData<Int?> = _currentWeek

    private val parser = Parser()
    private val httpClient = HttpClient()

    private val _currentGroupsList: MutableLiveData<Pair<List<String>, List<Int>?>> by lazy {
        MutableLiveData<Pair<List<String>, List<Int>?>>().also {
            if (repository.facultyIndex != TEACHER_INDEX)
                selectFaculty(repository.facultyIndex)
            else if (repository.teacherFio.isNotEmpty())
                selectTeacher(repository.teacherFio)
        }
    }

    val currentGroupsList: LiveData<Pair<List<String>, List<Int>?>> = _currentGroupsList

    init {
        handleException(true) { _currentWeek.value = repository.lastWeekNumber }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val weekNumber = parser.getWeekNumber()
                _currentWeek.postValue(weekNumber)
                repository.lastWeekNumber = weekNumber

                if (repository.useAnalyticsFunctions && weekNumber != repository.extraDataVersion) {
                    WorkManager.getInstance(application).beginUniqueWork(
                        SettingsViewModel.BUILD_MODEL,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(BuildModelWorker::class.java)
                    ).enqueue()
                }
            }
        }
    }

    private inline fun handleException(ignore: Boolean = false, block: () -> Unit) = try {
        block()
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

            val id = Constant.facultiesIds[index]

            handleException(true) {
                _currentGroupsList.postValue(repository.loadFacultyCache(id) to null)
            }

            val calendar = Calendar.getInstance()

            handleException {
                val data = parser.pickGroups(id, Constant.getSemester(calendar), Constant.getYear(calendar))
                _currentGroupsList.postValue(data to null)
                repository.saveFacultyCache(id, data)
            }

            _isLoading.postValue(false)
        }
    }

    fun selectGroup(group: String) {
        _isLoading.value = true

        var cache: ScheduleGetResult? = null

        handleException(true) {
            cache = repository.loadTimetableCache(group).apply {
                _currentTimetableError.value = createTimetableError(this)
                _currentTimetableInfo.value = createTimetableInfo(this)
            }
        }

        val calendar = Calendar.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                httpClient.scheduleGetJson(
                    group,
                    Constant.getSemester(calendar),
                    Constant.getYear(calendar)
                ).let {
                    _currentTimetableError.postValue(createTimetableError(it))
                    _currentTimetableInfo.postValue(createTimetableInfo(it, cache))
                    repository.saveTimetableCache(group, it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun selectTeacher(teacherId: Int) {
        _isLoading.value = true

        handleException(true) {
            repository.loadTimetableCache(teacherId.toString()).let {
                _currentTimetableError.value = createTimetableError(it)
                _currentTimetableInfo.value = createTimetableInfo(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val calendar = Calendar.getInstance()

                httpClient.scheduleGetTeacherJson(
                    teacherId,
                    Constant.getSemester(calendar),
                    Constant.getYear(calendar)
                ).let {
                    _currentTimetableError.postValue(createTimetableError(it))
                    _currentTimetableInfo.postValue(createTimetableInfo(it))
                    repository.saveTimetableCache(teacherId.toString(), it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun restoreTimetableFromCache(cacheKey: String) {
        _currentWeek.observeForever(object : Observer<Int?> {
            override fun onChanged(value: Int?) {
                repository.loadTimetableCache(cacheKey).let {
                    _currentTimetableError.value = createTimetableError(it)
                    _currentTimetableInfo.value = createTimetableInfo(it)
                    _isLoading.value = false
                }

                _currentWeek.removeObserver(this)
            }
        })
    }

    private fun createTimetableError(data: ScheduleGetResult) =
        TimetableError(
            data.title,
            data.message
        )
            .takeIf { data.status == Status.ERROR }

    private fun createTimetableInfo(data: ScheduleGetResult, cache: ScheduleGetResult? = null): TimetableInfo {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }

        val currentWeek = _currentWeek.value ?: calendar.get(Calendar.WEEK_OF_YEAR)
        var isEven = currentWeek % 2 == 0

        var todayIndex = 0

        val list = mutableListOf<TimetableItem>()

        val lastDay = if (data.disciplines.size > 1) {
            data.disciplines.keys.last().toInt()
        } else {
            1
        }

        val inverted = Constant.defaultWeek.indexOf(calendar.get(Calendar.DAY_OF_WEEK)) > lastDay - 1

        if (inverted)
            isEven = !isEven

        val showPrevGroup = repository.showPrevGroup
        val showTeacherPath = repository.showTeacherPath
        val disableFilter = repository.disableFilter
        var useAnalyticsFunctions = repository.useAnalyticsFunctions

        var warningShowed = false

        for (day in data.disciplines) {
            val dayIndex = day.key.toInt() - 1

            val isToday = Constant.defaultWeek.getOrNull(dayIndex) == calendar.get(Calendar.DAY_OF_WEEK)

            list.add(TimetableItem.DayHeader(dayIndex))

            if ((isToday || inverted) && !warningShowed) {
                todayIndex = dayIndex

                if (_currentWeek.value == null)
                    list.add(TimetableItem.Warning.CURRENT_WEEK_NULL)

                warningShowed = true
            }

            for (klass in day.value) {
                list.add(TimetableItem.ParaHeader(klass.key.toInt() - 1, isToday))

                for (week in klass.value) {
                    for ((index, para) in week.value.withIndex()) {
                        if (cache != null && useAnalyticsFunctions) {
                            val cachedPara =
                                cache.disciplines[day.key]?.get(klass.key)?.get(week.key)?.get(index)

                            if (cachedPara != null)
                                para.extraData = cachedPara.extraData
                            else
                                useAnalyticsFunctions = false
                        }

                        para.extraData?.run {
                            if (!showTeacherPath)
                                prevBuilding = null

                            if (!showPrevGroup) {
                                prevName = null
                                prevGroupName = null
                            }
                        }

                        val item = TimetableItem.ParaItem(para)

                        list.add(item)
                    }
                }
            }
        }

        val weekValue = _currentWeek.value?.plus(if (inverted) 1 else 0)

        val startDate = if (weekValue != null) {
            Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                add(Calendar.WEEK_OF_YEAR, -weekValue + 1)
            }
        } else null

        return TimetableInfo(
            timetable = list,
            isEven = isEven,
            todayIndex = todayIndex,
            currentWeek = weekValue,
            disableFilter = disableFilter,
            disableWeekClasses = repository.disableWeekClasses,
            startDate = startDate
        )
    }

    companion object {
        const val TEACHER_INDEX = 5
    }
}