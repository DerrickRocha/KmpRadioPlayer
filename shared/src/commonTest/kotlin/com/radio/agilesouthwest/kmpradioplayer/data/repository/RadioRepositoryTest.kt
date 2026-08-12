package com.radio.agilesouthwest.kmpradioplayer.data.repository

import com.radio.agilesouthwest.kmpradioplayer.data.network.RadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RadioRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeRadioApiService : RadioApiService {
        var shouldFail = false
        val stations = listOf(NetworkRadioStation("1", "Fake", "url", null, null, 128))
        val tags = listOf(NetworkTag("rock", 10))

        override suspend fun getStationsByTag(tag: String, limit: Int, offset: Int): List<NetworkRadioStation> {
            if (shouldFail) throw Exception("Network error")
            return stations
        }

        override suspend fun getAllTags(limit: Int, offset: Int): List<NetworkTag> {
            if (shouldFail) throw Exception("Network error")
            return tags
        }

        override suspend fun searchStations(tag: String, name: String, language: String, limit: Int, offset: Int): List<NetworkRadioStation> {
            if (shouldFail) throw Exception("Network error")
            return stations
        }

        override suspend fun getStationByUuid(uuid: String): NetworkRadioStation {
            if (shouldFail) throw Exception("Network error")
            return stations[0]
        }
    }

    @Test
    fun `getStationsByTag should return success Result`() = runTest {
        val fakeApi = FakeRadioApiService()
        val repository = RadioRepository(fakeApi, testDispatcher)
        
        val result = repository.getStationsByTag("rock")
        
        assertTrue(result.isSuccess)
        assertEquals("Fake", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `getStationsByTag should return failure Result when API fails`() = runTest {
        val fakeApi = FakeRadioApiService().apply { shouldFail = true }
        val repository = RadioRepository(fakeApi, testDispatcher)

        val result = repository.getStationsByTag("rock")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
