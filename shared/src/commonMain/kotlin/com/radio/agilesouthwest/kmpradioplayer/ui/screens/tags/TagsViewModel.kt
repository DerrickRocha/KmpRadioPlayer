package com.radio.agilesouthwest.kmpradioplayer.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

data class TagsUiState(
    val tags: List<NetworkTag> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)

class TagsViewModel(private val repository: RadioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 20
    private var searchJob: Job? = null

    init {
        loadNextPage()
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.searchQuery == query) return
        
        _uiState.update { it.copy(searchQuery = query, tags = emptyList(), endReached = false) }
        currentOffset = 0
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadNextPage()
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val result = if (query.isBlank()) {
                repository.getAllTags(limit = limit, offset = currentOffset)
            } else {
                repository.searchTags(tag = query, limit = limit, offset = currentOffset)
            }

            result.onSuccess { newTags ->
                    _uiState.update { state ->
                        state.copy(
                            tags = state.tags + newTags,
                            isLoading = false,
                            endReached = newTags.size < limit
                        )
                    }
                    currentOffset += limit
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
