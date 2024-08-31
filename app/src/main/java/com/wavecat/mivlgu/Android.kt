package com.wavecat.mivlgu

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlin.math.roundToInt

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        return getParcelable(key) as T?
    }
}

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()
val Float.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()