package com.wavecat.mivlgu

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.models.Status
import com.wavecat.mivlgu.preferences.BooleanPreference
import com.wavecat.mivlgu.preferences.IntPreference
import com.wavecat.mivlgu.preferences.StringPreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainRepository(context: Context) {

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val facultyCache: SharedPreferences =
        context.getSharedPreferences("faculty_cache", Context.MODE_PRIVATE)

    private val timetableCache: SharedPreferences =
        context.getSharedPreferences("timetable_cache", Context.MODE_PRIVATE)

    var extraDataVersion by IntPreference(preferences, EXTRA_DATA_VERSION, 0)
    var facultyIndex by IntPreference(preferences, FACULTY_INDEX, 0)
    var teacherFio by StringPreference(preferences, TEACHER_FIO, "")
    var disableFilter by BooleanPreference(preferences, DISABLE_FILTER, false)
    var disableAI by BooleanPreference(preferences, DISABLE_AI, true)
    var disableWeekClasses by BooleanPreference(preferences, DISABLE_WEEK_CLASSES, false)
    var disableIEP by BooleanPreference(preferences, DISABLE_IEP, false)
    var showPrevGroup by BooleanPreference(preferences, SHOW_PREV_GROUP, false)
    var showTeacherPath by BooleanPreference(preferences, SHOW_TEACHER_PATH, false)
    var showExperiments by BooleanPreference(preferences, SHOW_EXPERIMENTS, false)
    var donationMade by BooleanPreference(preferences, DONATION_MADE, false)

    init {
        // migration

        if (preferences.getInt("version", -1) != -1)
            disableFilter = preferences.getBoolean(DISABLE_FILTER, false)

        if (useAnalyticsFunctions)
            showExperiments = true
    }

    val useAnalyticsFunctions: Boolean get() = showPrevGroup || showTeacherPath

    var lastWeekNumber: Int?
        get() = preferences.getInt(LAST_WEEK_NUMBER, -1).takeIf { it != -1 }
        set(value) = preferences.edit {
            putInt(LAST_WEEK_NUMBER, value ?: -1)
            apply()
        }

    fun saveFacultyCache(facultyId: Int, data: List<String>) = facultyCache.edit {
        putString(facultyId.toString(), Json.encodeToString(data))
        apply()
    }

    fun loadFacultyCache(facultyId: Int): List<String> =
        Json.decodeFromString(facultyCache.getString(facultyId.toString(), "[]")!!)

    fun saveTimetableCache(name: String, data: ScheduleGetResult) =
        timetableCache.edit {
            putString(name, Json.encodeToString(data))
            apply()
        }

    fun getAllCachedGroups() = buildList {
        Constant.facultiesIds.forEach { id ->
            addAll(loadFacultyCache(id))
        }
    }

    fun loadTimetableCache(name: String): ScheduleGetResult {
        val result = timetableCache.getString(name, "")
        return if (result.isNullOrEmpty()) emptyScheduleGetResult
        else Json.decodeFromString(result)
    }

    companion object {
        const val FACULTY_INDEX = "faculty_index"
        const val TEACHER_FIO = "teacher_fio"
        const val LAST_WEEK_NUMBER = "last_week_number"

        const val EXTRA_DATA_VERSION = "extra_data_version"

        const val DISABLE_FILTER = "disable_filter"
        const val DISABLE_AI = "disable_ai"
        const val DISABLE_WEEK_CLASSES = "disable_week_classes"
        const val DISABLE_IEP = "disable_iep"

        const val SHOW_PREV_GROUP = "show_prev_group"
        const val SHOW_TEACHER_PATH = "show_teacher_path"

        const val SHOW_EXPERIMENTS = "show_experiments"
        const val DONATION_MADE = "donation_made"

        val emptyScheduleGetResult = ScheduleGetResult(
            status = Status.OK,
            time = "",
            group = null,
            teacher = null,
            title = "",
            message = "",
            semestr = "",
            year = "",
            disciplines = mapOf()
        )
    }
}