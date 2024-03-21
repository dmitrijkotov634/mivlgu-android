package com.wavecat.mivlgu

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.Static
import com.wavecat.mivlgu.client.Status
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainRepository(context: Context) {

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val facultyCache: SharedPreferences =
        context.getSharedPreferences("faculty_cache", Context.MODE_PRIVATE)

    private val timetableCache: SharedPreferences =
        context.getSharedPreferences("timetable_cache", Context.MODE_PRIVATE)

    var version: Int
        get() = preferences.getInt(VERSION, 0)
        set(value) = preferences.edit {
            putInt(VERSION, value)
            apply()
        }

    var extraDataVersion: Int
        get() = preferences.getInt(EXTRA_DATA_VERSION, 0)
        set(value) = preferences.edit {
            putInt(EXTRA_DATA_VERSION, value)
            apply()
        }

    init {
        if (version != BuildConfig.VERSION_CODE) {
            version = BuildConfig.VERSION_CODE
            extraDataVersion = 0
        }
    }

    var facultyIndex: Int
        get() = preferences.getInt(FACULTY_INDEX, 0)
        set(value) = preferences.edit {
            putInt(FACULTY_INDEX, value)
            apply()
        }

    var teacherFio: String?
        get() = preferences.getString(TEACHER_FIO, "")
        set(value) = preferences.edit {
            putString(TEACHER_FIO, value)
            apply()
        }

    var lastWeekNumber: Int?
        get() = preferences.getInt(LAST_WEEK_NUMBER, -1).takeIf { it != -1 }
        set(value) = preferences.edit {
            putInt(LAST_WEEK_NUMBER, value ?: -1)
            apply()
        }

    var disableFilter: Boolean
        get() = preferences.getBoolean(DISABLE_FILTER, false)
        set(value) = preferences.edit {
            putBoolean(DISABLE_FILTER, value)
            apply()
        }

    var disableAI: Boolean
        get() = preferences.getBoolean(DISABLE_AI, true)
        set(value) = preferences.edit {
            putBoolean(DISABLE_AI, value)
            apply()
        }

    var disableWeekClasses: Boolean
        get() = preferences.getBoolean(DISABLE_WEEK_CLASSES, false)
        set(value) = preferences.edit {
            putBoolean(DISABLE_WEEK_CLASSES, value)
            apply()
        }

    var disableIEP: Boolean
        get() = preferences.getBoolean(DISABLE_IEP, false)
        set(value) = preferences.edit {
            putBoolean(DISABLE_IEP, value)
            apply()
        }

    var showPrevGroup: Boolean
        get() = preferences.getBoolean(SHOW_PREV_GROUP, false)
        set(value) = preferences.edit {
            putBoolean(SHOW_PREV_GROUP, value)
            apply()
        }

    var showTeacherPath: Boolean
        get() = preferences.getBoolean(SHOW_TEACHER_PATH, false)
        set(value) = preferences.edit {
            putBoolean(SHOW_TEACHER_PATH, value)
            apply()
        }

    val useAnalyticsFunctions: Boolean
        get() = showPrevGroup || showTeacherPath

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
        Static.facultiesIds.forEach { id ->
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
        const val VERSION = "version"

        const val DISABLE_FILTER = "disable_filter"
        const val DISABLE_AI = "disable_ai"
        const val DISABLE_WEEK_CLASSES = "disable_week_classes"
        const val DISABLE_IEP = "disable_iep"

        const val SHOW_PREV_GROUP = "show_prev_group"
        const val SHOW_TEACHER_PATH = "show_teacher_path"

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