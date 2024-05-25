package com.wavecat.mivlgu.client.models

import kotlinx.serialization.Serializable

@Serializable
data class Teacher(
    val id: Int,
    val name: String,
)