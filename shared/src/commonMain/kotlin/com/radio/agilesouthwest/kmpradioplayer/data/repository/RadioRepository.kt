package com.radio.agilesouthwest.kmpradioplayer.data.repository

import com.radio.agilesouthwest.kmpradioplayer.data.network.RadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag

class RadioRepository(private val apiService: RadioApiService) {

    suspend fun getStationsByTag(tag: String, limit: Int = 20, offset: Int = 0): Result<List<NetworkRadioStation>> =
        runCatching { apiService.getStationsByTag(tag, limit, offset) }

    suspend fun getAllTags(limit: Int = 20, offset: Int = 0): Result<List<NetworkTag>> =
        runCatching { apiService.getAllTags(limit, offset) }

    suspend fun searchStations(
        tag: String = "",
        name: String = "",
        language: String = "",
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<NetworkRadioStation>> =
        runCatching { apiService.searchStations(tag, name, language, limit, offset) }

    suspend fun getStationByUuid(uuid: String): Result<NetworkRadioStation> =
        runCatching { apiService.getStationByUuid(uuid) }
}
