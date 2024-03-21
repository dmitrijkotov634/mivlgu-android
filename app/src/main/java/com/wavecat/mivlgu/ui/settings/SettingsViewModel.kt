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

    private val _useAnalyticsFunctions = MutableLiveData(repository.useAnalyticsFunctions)
    val useAnalyticsFunctions: LiveData<Boolean> = _useAnalyticsFunctions

    fun showAnalyticsFunctions() {
        _useAnalyticsFunctions.value = true
    }

    fun changeShowPrevGroups(state: Boolean) {
        if (state) buildModel()
        _showPrevGroups.value = state
        repository.showPrevGroup = state
    }

    private val _showTeacherPath = MutableLiveData(repository.showTeacherPath)
    val showTeacherPath: LiveData<Boolean> = _showTeacherPath

    fun changeShowTeacherPath(state: Boolean) {
        if (state) buildModel()
        _showTeacherPath.value = state
        repository.showTeacherPath = state
    }

    private val _disableFilter = MutableLiveData(repository.disableFilter)
    val disableFilter: LiveData<Boolean> = _disableFilter

    fun changeDisableFilter(state: Boolean) {
        _disableFilter.value = state
        repository.disableFilter = state
    }

    private val _disableWeekClasses = MutableLiveData(repository.disableWeekClasses)
    val disableWeekClasses: LiveData<Boolean> = _disableWeekClasses

    fun changeDisableWeekClasses(state: Boolean) {
        _disableWeekClasses.value = state
        repository.disableWeekClasses = state
    }

    private val _disableIEP = MutableLiveData(repository.disableIEP)
    val disableIEP: LiveData<Boolean> = _disableIEP

    fun changeDisableIEP(state: Boolean) {
        _disableIEP.value = state
        repository.disableIEP = state
    }

    private val _disableAI = MutableLiveData(repository.disableAI)
    val disableAI: LiveData<Boolean> = _disableAI

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
        repository.saveTimetableCache(EASTER_EGG_KEY, EasterEgg.generate())
    }

    companion object {
        const val BUILD_MODEL = "build_model"
        const val PRELOAD = "preload"
        const val EASTER_EGG_KEY = "easter_egg"
    }
}
