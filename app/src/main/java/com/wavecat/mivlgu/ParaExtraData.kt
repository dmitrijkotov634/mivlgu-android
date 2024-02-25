package com.wavecat.mivlgu

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ParaExtraData(
    var prevGroupName: String? = null,
    var prevName: String? = null,
    var prevBuilding: String? = null
) : Parcelable {
    fun isEmpty() = prevBuilding == null
            && prevGroupName == null
            && prevName == null
}
