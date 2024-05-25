package com.wavecat.mivlgu.client.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WeekType {
    @SerialName("even")
    EVEN,

    @SerialName("odd")
    ODD,

    @SerialName("all")
    ALL
}