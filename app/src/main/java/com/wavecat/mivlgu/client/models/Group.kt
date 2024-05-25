package com.wavecat.mivlgu.client.models

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
)