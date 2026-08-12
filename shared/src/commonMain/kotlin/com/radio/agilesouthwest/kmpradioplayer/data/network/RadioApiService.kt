package com.radio.agilesouthwest.kmpradioplayer.data.network

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag

interface RadioApiService {
    suspend fun getStationsByTag(tag: String, limit: Int = 20, offset: Int = 0): List<NetworkRadioStation>
    suspend fun getAllTags(limit: Int = 20, offset: Int = 0): List<NetworkTag>
    suspend fun searchStations(
        tag: String = "",
        name: String = "",
        language: String = "",
        limit: Int = 20,
        offset: Int = 0
    ): List<NetworkRadioStation>
    suspend fun getStationByUuid(uuid: String): NetworkRadioStation
}
