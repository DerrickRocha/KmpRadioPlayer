package com.radio.agilesouthwest.kmpradioplayer.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkTag
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagsUiState(
    val tags: List<NetworkTag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)

class TagsViewModel(private val repository: RadioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 20

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            repository.getAllTags(limit = limit, offset = currentOffset)
                .onSuccess { newTags ->
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
