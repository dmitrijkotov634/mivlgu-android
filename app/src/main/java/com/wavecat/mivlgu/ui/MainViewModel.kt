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
import com.wavecat.mivlgu.client.models.WeekType
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
        // Получаем текущую учебную неделю из кэша
        handleException(true) { _currentWeek.value = repository.lastWeekNumber }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                // Получаем текущую учебную неделю по API
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

            // Загружаем список групп из кэша
            handleException(true) {
                _currentGroupsList.postValue(repository.loadFacultyCache(id) to null)
            }

            val calendar = Calendar.getInstance()

            handleException {
                // Получаем актуальный список групп и записываем в кэш
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

        // Загружаем расписание с кэша
        handleException(true) {
            cache = repository.loadTimetableCache(group).apply {
                _currentTimetableError.value = createTimetableError(this)
                _currentTimetableInfo.value = createTimetableInfo(this)
            }
        }

        val calendar = Calendar.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                // Получаем актуальное расписание и записываем в кэш
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
            // Загружаем расписание с кэша
            repository.loadTimetableCache(teacherId.toString()).let {
                _currentTimetableError.value = createTimetableError(it)
                _currentTimetableInfo.value = createTimetableInfo(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                val calendar = Calendar.getInstance()

                // Получаем актуальное расписание и записываем в кэш
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
        val calendar = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }

        // Получаем текущую учебную неделю по API иначе по времени на устройстве
        val currentWeek = _currentWeek.value ?: calendar.get(Calendar.WEEK_OF_YEAR)
        var isEven = currentWeek % 2 == 0

        var todayIndex = 0

        val list = mutableListOf<TimetableItem>()

        // Получаем последний день в расписании
        val lastDay = if (data.disciplines.size > 1) {
            data.disciplines.keys.last().toInt()
        } else {
            1
        }

        // Инвертируем четность недели если расписание на текущей недел закончилось
        val inverted = Constant.defaultWeek.indexOf(calendar.get(Calendar.DAY_OF_WEEK)) > lastDay - 1

        if (inverted)
            isEven = !isEven

        // Предпочтения пользователя
        val showPrevGroup = repository.showPrevGroup
        val showTeacherPath = repository.showTeacherPath
        var useAnalyticsFunctions = repository.useAnalyticsFunctions

        var disableFilter = repository.disableFilter
        var disableWeekClasses = repository.disableWeekClasses

        var hasInvalidRanges = false

        for (day in data.disciplines) {
            val dayIndex = day.key.toInt() - 1

            // Проверяем является ли день в расписании сегодняшним
            val isToday = Constant.defaultWeek.getOrNull(dayIndex) == calendar.get(Calendar.DAY_OF_WEEK)

            list.add(TimetableItem.DayHeader(dayIndex))

            if (isToday)
                todayIndex = dayIndex // Установили позицию для скролла

            for (klass in day.value) {
                list.add(TimetableItem.ParaHeader(klass.key.toInt() - 1, isToday))

                for (week in klass.value) {
                    for ((index, para) in week.value.withIndex()) {
                        // Обработка аналитических функций
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

                        list.add(TimetableItem.ParaItem(para))

                        // Проверяем наличие некорректных перечислений учебных недель
                        runCatching {
                            para.isToday(WeekType.ALL, currentWeek)
                        }.onFailure {
                            hasInvalidRanges = true
                            it.printStackTrace()
                        }
                    }
                }
            }
        }

        // Выводим предупреждения пользователю
        if (_currentWeek.value == null)
            list.add(todayIndex, TimetableItem.Warning.CURRENT_WEEK_NULL)

        if (hasInvalidRanges)
            list.add(todayIndex, TimetableItem.Warning.HAS_INVALID_RANGES)

        // Инкрементируем значение недели если показываем следующую
        val weekValue = _currentWeek.value?.plus(if (inverted) 1 else 0)

        // Получаем первую учебную неделю, вычитая текущую дату на устройстве и текущую неделю по API
        var startDate: Calendar? = null
        if (_currentWeek.value != null) {
            startDate = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                add(Calendar.WEEK_OF_YEAR, -(_currentWeek.value)!!)
            }
        }

        // Отключаем функции фильтрации расписания если при парсинге были обнаружены ошибки
        if (hasInvalidRanges) {
            disableFilter = true
            disableWeekClasses = true
        }

        return TimetableInfo(
            timetable = list,
            isEven = isEven,
            todayIndex = todayIndex,
            currentWeek = weekValue,
            disableFilter = disableFilter,
            disableWeekClasses = disableWeekClasses,
            startDate = startDate,
            hasInvalidRanges = hasInvalidRanges
        )
    }

    companion object {
        const val TEACHER_INDEX = 5
    }
}