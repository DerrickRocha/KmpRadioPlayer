package com.radio.agilesouthwest.kmpradioplayer.data.repository

import com.radio.agilesouthwest.kmpradioplayer.data.network.RadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RadioRepository(
    private val apiService: RadioApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getStationsByTag(tag: String, limit: Int = 20, offset: Int = 0): Result<List<NetworkRadioStation>> =
        withContext(dispatcher) {
            runCatching { apiService.getStationsByTag(tag, limit, offset) }
        }

    suspend fun getAllTags(limit: Int = 20, offset: Int = 0): Result<List<NetworkTag>> =
        withContext(dispatcher) {
            runCatching { apiService.getAllTags(limit, offset) }
        }

    suspend fun searchTags(tag: String, limit: Int = 20, offset: Int = 0): Result<List<NetworkTag>> =
        withContext(dispatcher) {
            runCatching { apiService.searchTags(tag, limit, offset) }
        }

    suspend fun searchStations(
        tag: String = "",
        name: String = "",
        language: String = "",
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<NetworkRadioStation>> =
        withContext(dispatcher) {
            runCatching { apiService.searchStations(tag, name, language, limit, offset) }
        }

    suspend fun getStationByUuid(uuid: String): Result<NetworkRadioStation> =
        withContext(dispatcher) {
            runCatching { apiService.getStationByUuid(uuid) }
        }
}
