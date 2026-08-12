package com.radio.agilesouthwest.kmpradioplayer.ui.screens.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationsUiState(
    val stations: List<NetworkRadioStation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)

class StationsViewModel(
    private val repository: RadioRepository,
    private val tagName: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 20

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            repository.searchStations(
                tag = tagName ?: "",
                limit = limit,
                offset = currentOffset
            ).onSuccess { newStations ->
                _uiState.update { state ->
                    state.copy(
                        stations = state.stations + newStations,
                        isLoading = false,
                        endReached = newStations.size < limit
                    )
                }
                currentOffset += limit
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
