package com.radio.agilesouthwest.kmpradioplayer.data.network

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorRadioApiService(private val httpClient: HttpClient) : RadioApiService {
    
    override suspend fun getStationsByTag(tag: String, limit: Int, offset: Int): List<NetworkRadioStation> {
        return httpClient.get("Radio/stations/$tag") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    override suspend fun getAllTags(limit: Int, offset: Int): List<NetworkTag> {
        return httpClient.get("Radio/tags/all") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    override suspend fun searchStations(
        tag: String,
        name: String,
        language: String,
        limit: Int,
        offset: Int
    ): List<NetworkRadioStation> {
        return httpClient.get("Radio/stations/search") {
            parameter("tag", tag)
            parameter("name", name)
            parameter("language", language)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    override suspend fun getStationByUuid(uuid: String): NetworkRadioStation {
        return httpClient.get("Radio/stations/uuid/$uuid").body()
    }
}
