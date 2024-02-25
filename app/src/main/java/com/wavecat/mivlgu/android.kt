package com.wavecat.mivlgu

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        return getParcelable(key) as T?
    }
}