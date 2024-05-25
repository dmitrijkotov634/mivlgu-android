package com.wavecat.mivlgu.client.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    @SerialName("error")
    ERROR,

    @SerialName("ok")
    OK,
}