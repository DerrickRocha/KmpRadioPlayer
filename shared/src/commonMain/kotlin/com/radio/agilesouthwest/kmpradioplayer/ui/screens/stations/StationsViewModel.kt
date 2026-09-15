package com.radio.agilesouthwest.kmpradioplayer.ui.screens.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class StationsUiState(
    val stations: List<NetworkRadioStation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val searchQuery: String = ""
)
class StationsViewModel(
    private val repository: RadioRepository,
    tagName: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 20
    private var currentTag = tagName

    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var requestGeneration = 0

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        val generation = requestGeneration
        _uiState.update { it.copy(isLoading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.searchStations(
                tag = currentTag ?: "",
                name = uiState.value.searchQuery,
                limit = limit,
                offset = currentOffset
            ).onSuccess { newStations ->
                if (generation != requestGeneration) return@onSuccess // stale response, discard
                _uiState.update { state ->
                    state.copy(
                        stations = state.stations + newStations,
                        isLoading = false,
                        endReached = newStations.size < limit
                    )
                }
                currentOffset += limit
            }.onFailure { error ->
                if (generation != requestGeneration) return@onFailure
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.searchQuery == query) return

        requestGeneration++
        loadJob?.cancel()
        searchJob?.cancel()

        currentOffset = 0
        _uiState.update {
            it.copy(
                searchQuery = query,
                stations = emptyList(),
                endReached = false,
                isLoading = false,
                error = null
            )
        }

        searchJob = viewModelScope.launch {
            delay(300.milliseconds)
            loadNextPage()
        }
    }
}
