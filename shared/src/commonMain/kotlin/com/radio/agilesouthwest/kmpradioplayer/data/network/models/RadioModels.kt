package com.radio.agilesouthwest.kmpradioplayer.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkTag(
    val name: String,
    val stationCount: Int
)

@Serializable
data class NetworkRadioStation(
    val stationUuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String?,
    val tags: String?,
    val bitrate: Int
)
