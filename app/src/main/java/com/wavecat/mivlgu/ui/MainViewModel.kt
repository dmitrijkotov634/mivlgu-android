package com.wavecat.mivlgu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wavecat.mivlgu.Constant
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.client.HttpClient
import com.wavecat.mivlgu.client.Parser
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.models.Status
import com.wavecat.mivlgu.client.models.getMaxWeekNumber
import com.wavecat.mivlgu.ui.settings.SettingsViewModel
import com.wavecat.mivlgu.ui.timetable.TimetableItem
import com.wavecat.mivlgu.workers.BuildModelWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.rustore.sdk.remoteconfig.RemoteConfigClient
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MainRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentFacultyIndex = MutableLiveData<Int>()
    val currentFacultyIndex: LiveData<Int> = _currentFacultyIndex

    private val _currentTimetableInfo = MutableLiveData<TimetableInfo?>()
    val currentTimetableInfo: LiveData<TimetableInfo?> = _currentTimetableInfo

    private val _loadingException = MutableLiveData<Exception?>()
    val loadingException: LiveData<Exception?> = _loadingException

    private val _currentWeek = MutableLiveData<Int?>()
    val currentWeek: LiveData<Int?> = _currentWeek

    private val parser = Parser()
    private val httpClient = HttpClient()

    private val _currentGroupsList: MutableLiveData<Pair<List<String>, List<Int>?>> by lazy {
        MutableLiveData<Pair<List<String>, List<Int>?>>().apply {
            when {
                repository.facultyIndex != TEACHER_INDEX -> selectFaculty(repository.facultyIndex)
                repository.teacherFio.isNotEmpty() -> findTeacher(repository.teacherFio)
            }
        }
    }

    val currentGroupsList: LiveData<Pair<List<String>, List<Int>?>> = _currentGroupsList

    private var showDatesAndHints = true
    private var forceMonday = false

    init {
        handleException(true) { _currentWeek.value = repository.cachedWeekNumber }

        viewModelScope.launch(Dispatchers.IO) {
            handleException {
                var weekNumber = parser.getWeekNumber()

                runCatching {
                    val remoteConfig = getRemoteConfig()

                    val forceWeekNumber = remoteConfig.getInt("force_week_number")
                    showDatesAndHints = !remoteConfig.getBoolean("hide_dates_and_hints")
                    repository.allowAssistant = remoteConfig.getBoolean("allow_assistant")

                    if (forceWeekNumber > 0)
                        weekNumber = forceWeekNumber

                    if (remoteConfig.getBoolean("drop_all_cache"))
                        repository.dropAllCache()

                    forceMonday = remoteConfig.getBoolean("force_monday")
                }
                    .onFailure {
                        it.printStackTrace()
                    }

                _currentWeek.postValue(weekNumber)
                repository.cachedWeekNumber = weekNumber

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

    private fun getRemoteConfig() = RemoteConfigClient.instance
        .getRemoteConfig()
        .await()

    fun findTeacher(fio: String?) {
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
                _currentGroupsList.postValue(repository.retrieveFacultyCache(id) to null)
            }

            val calendar = Calendar.getInstance()

            handleException {
                val data = parser.pickGroups(
                    id,
                    Constant.getSemester(calendar),
                    Constant.getYear(calendar)
                )
                _currentGroupsList.postValue(data to null)
                repository.cacheFacultyData(id, data)
            }

            _isLoading.postValue(false)
        }
    }

    fun selectGroup(group: String) {
        _isLoading.value = true

        var cache: ScheduleGetResult? = null

        handleException(true) {
            cache = repository.retrieveTimetableCache(group).apply {
                _currentTimetableInfo.value = processTimetableInfo(this)
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
                    _currentTimetableInfo.postValue(processTimetableInfo(it, cache))
                    repository.cacheTimetableData(group, it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun selectTeacher(teacherId: Int) {
        _isLoading.value = true

        handleException(true) {
            repository.retrieveTimetableCache(teacherId.toString()).let {
                _currentTimetableInfo.value = processTimetableInfo(it)
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
                    _currentTimetableInfo.postValue(processTimetableInfo(it))
                    repository.cacheTimetableData(teacherId.toString(), it)
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun restoreTimetableFromCache(cacheKey: String) {
        _currentWeek.observeForever(object : Observer<Int?> {
            override fun onChanged(value: Int?) {
                repository.retrieveTimetableCache(cacheKey).let {
                    _currentTimetableInfo.value = processTimetableInfo(it)
                    _isLoading.value = false
                }

                _currentWeek.removeObserver(this)
            }
        })
    }

    fun processTimetableInfo(
        data: ScheduleGetResult,
        cache: ScheduleGetResult? = null
    ): TimetableInfo {
        if (data.status == Status.ERROR)
            return TimetableInfo.Failure(data.title, data.message)

        val calendar = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }

        val currentWeek = _currentWeek.value ?: calendar.get(Calendar.WEEK_OF_YEAR)
        var isEven = currentWeek % 2 == 0

        var todayIndex = 0
        var todayItemIndex = 0

        val list = mutableListOf<TimetableItem>()

        val lastDay = if (data.disciplines.size > 1) {
            data.disciplines.keys.last().toInt()
        } else {
            1
        }

        val inverted =
            Constant.defaultWeek.indexOf(calendar.get(Calendar.DAY_OF_WEEK)) > lastDay - 1

        if (inverted)
            isEven = !isEven

        val showPrevGroup = repository.showPrevGroup
        val showTeacherPath = repository.showTeacherPath
        val showRouteTime = repository.showRouteTime
        val useAnalyticsFunctions = repository.useAnalyticsFunctions

        var showWeekParityFilter = repository.showWeekParityFilter
        var disableWeekClasses = repository.disableWeekClasses
        var showCurrentWeek = repository.showWeekChooser

        var hasInvalidRanges = false

        var maxWeekNumber = 1

        data.disciplines.forEach { (dayKey, classes) ->
            val dayIndex = dayKey.toInt() - 1

            val isToday =
                Constant.defaultWeek.getOrNull(dayIndex) == calendar.get(Calendar.DAY_OF_WEEK)

            list.add(TimetableItem.DayHeader(dayIndex))

            if (isToday && !forceMonday) {
                todayIndex = dayIndex
                todayItemIndex = list.size
            }

            classes.forEach { (classKey, weeks) ->
                list.add(TimetableItem.ParaHeader(classKey.toInt() - 1, isToday))

                weeks.forEach { (weekKey, paras) ->
                    paras.forEachIndexed { index, para ->
                        cache?.disciplines?.get(dayKey)?.get(classKey)?.get(weekKey)?.get(index)
                            ?.let { cachedPara ->
                                if (useAnalyticsFunctions) para.extraData = cachedPara.extraData
                            }

                        para.extraData?.let { extra ->
                            extra.prevBuilding = extra.takeIf { showTeacherPath }?.prevBuilding
                            extra.routeTime = extra.takeIf { showRouteTime }?.routeTime
                            if (!showPrevGroup) {
                                extra.prevName = null
                                extra.prevGroupName = null
                            }
                        }

                        runCatching {
                            val maxWeek = maxOf(
                                para.parsedWeekNumber.getMaxWeekNumber(),
                                para.parsedSubGroup1.getMaxWeekNumber(),
                                para.parsedSubGroup2.getMaxWeekNumber()
                            )

                            println(maxWeek)

                            if (maxWeek > maxWeekNumber)
                                maxWeekNumber = maxWeek
                        }.onFailure { e ->
                            hasInvalidRanges = true
                            e.printStackTrace()
                        }

                        list.add(TimetableItem.ParaItem(para))
                    }
                }
            }
        }

        if (_currentWeek.value == null) {
            list.add(todayItemIndex, TimetableItem.Warning.CURRENT_WEEK_NULL)
            showCurrentWeek = false
        }

        if (hasInvalidRanges) {
            list.add(todayItemIndex, TimetableItem.Warning.HAS_INVALID_RANGES)
            showWeekParityFilter = false
            disableWeekClasses = true
        }

        if (disableWeekClasses)
            showCurrentWeek = false

        val weekValue = _currentWeek.value?.plus(if (inverted) 1 else 0)

        val startDate = _currentWeek.value?.let {
            Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                add(Calendar.WEEK_OF_YEAR, -it)
            }
        }

        return TimetableInfo.Success(
            timetable = list,
            isEven = isEven,
            todayIndex = todayIndex,
            currentWeek = weekValue,
            showWeekParityFilter = showWeekParityFilter,
            disableWeekClasses = disableWeekClasses,
            startDate = startDate,
            hasInvalidRanges = hasInvalidRanges,
            showCurrentWeek = showCurrentWeek,
            showDatesAndCurrentKlassHints = showDatesAndHints,
            maxWeekNumber = maxWeekNumber
        )
    }

    companion object {
        const val TEACHER_INDEX = 5
    }
}