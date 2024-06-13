package com.wavecat.mivlgu.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.workers.BuildModelWorker
import com.wavecat.mivlgu.workers.PreloadWorker

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    private val repository = MainRepository(application)

    private val _showPrevGroups = MutableLiveData(repository.showPrevGroup)
    val showPrevGroups: LiveData<Boolean> = _showPrevGroups

    private val _showExperiments = MutableLiveData(repository.showExperiments)
    val showExperiments: LiveData<Boolean> = _showExperiments

    private val _showTeacherPath = MutableLiveData(repository.showTeacherPath)
    val showTeacherPath: LiveData<Boolean> = _showTeacherPath

    private val _showRouteTime = MutableLiveData(repository.showRouteTime)
    val showRouteTime: LiveData<Boolean> = _showRouteTime

    private val _disableFilter = MutableLiveData(repository.disableFilter)
    val disableFilter: LiveData<Boolean> = _disableFilter

    private val _showCurrentWeek = MutableLiveData(repository.showCurrentWeek)
    val showCurrentWeek: LiveData<Boolean> = _showCurrentWeek

    private val _disableWeekClasses = MutableLiveData(repository.disableWeekClasses)
    val disableWeekClasses: LiveData<Boolean> = _disableWeekClasses

    private val _disableIEP = MutableLiveData(repository.disableIEP)
    val disableIEP: LiveData<Boolean> = _disableIEP

    private val _disableAI = MutableLiveData(repository.disableAI)
    val disableAI: LiveData<Boolean> = _disableAI

    fun showExperiments() {
        repository.showExperiments = true
        _showExperiments.value = true
    }

    fun changeShowPrevGroups(state: Boolean) {
        if (state) buildModel()
        _showPrevGroups.value = state
        repository.showPrevGroup = state
    }

    fun changeShowTeacherPath(state: Boolean) {
        if (state) buildModel()
        _showTeacherPath.value = state
        repository.showTeacherPath = state
    }

    fun changeShowRouteTime(state: Boolean) {
        if (state) buildModel()
        _showRouteTime.value = state
        repository.showRouteTime = state
    }

    fun changeDisableFilter(state: Boolean) {
        _disableFilter.value = state
        repository.disableFilter = state
    }

    fun changeShowCurrentWeek(state: Boolean) {
        _showCurrentWeek.value = state
        repository.showCurrentWeek = state
    }

    fun changeDisableWeekClasses(state: Boolean) {
        _disableWeekClasses.value = state
        repository.disableWeekClasses = state
    }

    fun changeDisableIEP(state: Boolean) {
        _disableIEP.value = state
        repository.disableIEP = state
    }

    fun changeDisableAI(state: Boolean) {
        _disableAI.value = state
        repository.disableAI = state
    }

    private fun buildModel() {
        workManager.beginUniqueWork(
            BUILD_MODEL,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.from(BuildModelWorker::class.java)
        ).enqueue()
    }

    fun preload() {
        workManager.beginUniqueWork(
            PRELOAD,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.from(PreloadWorker::class.java)
        )
            .then(
                OneTimeWorkRequest.from(BuildModelWorker::class.java)
            ).enqueue()
    }

    fun generateEasterEgg() {
        repository.cacheTimetableData(EASTER_EGG_KEY, EasterEgg.generate())
    }

    companion object {
        const val BUILD_MODEL = "build_model"
        const val PRELOAD = "preload"
        const val EASTER_EGG_KEY = "easter_egg"
    }
}
