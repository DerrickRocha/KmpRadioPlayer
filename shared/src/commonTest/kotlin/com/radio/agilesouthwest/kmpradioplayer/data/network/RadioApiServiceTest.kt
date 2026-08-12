package com.radio.agilesouthwest.kmpradioplayer.data.network

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RadioApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `getStationsByTag should return list of stations`() = runTest {
        val mockStations = listOf(
            NetworkRadioStation("1", "Station 1", "url1", "fav1", "tag1", 128),
            NetworkRadioStation("2", "Station 2", "url2", "fav2", "tag2", 192)
        )
        
        val mockEngine = MockEngine { request ->
            assertEquals("/radio/stations/rock", request.url.encodedPath)
            assertEquals("20", request.url.parameters["limit"])
            assertEquals("0", request.url.parameters["offset"])
            
            respond(
                content = json.encodeToString(mockStations),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        
        val apiService = KtorRadioApiService(httpClient)
        val result = apiService.getStationsByTag("rock")
        
        assertEquals(2, result.size)
        assertEquals("Station 1", result[0].name)
    }

    @Test
    fun `getAllTags should return list of tags`() = runTest {
        val mockTags = listOf(
            NetworkTag("rock", 100),
            NetworkTag("pop", 200)
        )

        val mockEngine = MockEngine { request ->
            assertEquals("/radio/tags/all", request.url.encodedPath)
            
            respond(
                content = json.encodeToString(mockTags),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val apiService = KtorRadioApiService(httpClient)
        val result = apiService.getAllTags()

        assertEquals(2, result.size)
        assertEquals("rock", result[0].name)
        assertEquals(100, result[0].stationCount)
    }
}
