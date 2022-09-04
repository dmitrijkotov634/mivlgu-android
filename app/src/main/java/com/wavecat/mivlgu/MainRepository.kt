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

    companion object {
        const val FACULTY_INDEX = "faculty_index"
    }
}