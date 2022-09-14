package com.wavecat.mivlgu

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class MainRepository(context: Context) : Repository {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    override var facultyIndex: Int
        get() = preferences.getInt(FACULTY_INDEX, 0)
        set(value) = preferences.edit {
            putInt(FACULTY_INDEX, value)
            apply()
        }

    override var teacherFio: String?
        get() = preferences.getString(TEACHER_FIO, "")
        set(value) = preferences.edit {
            putString(TEACHER_FIO, value)
            apply()
        }

    companion object {
        const val FACULTY_INDEX = "faculty_index"
        const val TEACHER_FIO = "teacher_fio"
    }
}