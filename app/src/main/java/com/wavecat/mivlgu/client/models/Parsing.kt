package com.wavecat.mivlgu.client.models

import android.util.Log
import com.wavecat.mivlgu.BuildConfig
import com.wavecat.mivlgu.client.models.WeekRange.*

fun parseParityRange(string: String): WeekParityRange {
    val parts = string
        .split("-", "/")
        .map { it.toInt() }

    return WeekParityRange(parts[0], parts[1])
}

fun parseEnumeration(string: String) = WeekEnumeration(buildList {
    val prepared = string
        .trim()
        .removePrefix(",")
        .removeSuffix(",")

    if (prepared.isEmpty()) {
        Log.w(BuildConfig.APPLICATION_ID, "Empty string")
        return@buildList
    }

    prepared
        .split(",")
        .forEach {
            it.let { week ->
                add(
                    if (week.contains("-") || week.contains("/"))
                        parseParityRange(week)
                    else if (week.toIntOrNull() != null)
                        Week(week.toInt())
                    else {
                        Log.w(BuildConfig.APPLICATION_ID, "Invalid range in \"$string\"")
                        InvalidRange(week)
                    }
                )
            }
        }
})

