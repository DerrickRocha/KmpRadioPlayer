package com.radio.agilesouthwest.kmpradioplayer.media

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val currentStation: NetworkRadioStation? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isSeekable: Boolean = false,
    val error: String? = null
)

interface RadioPlayer {
    val state: StateFlow<PlaybackState>
    
    fun play(station: NetworkRadioStation)
    fun pause()
    fun resume()
    fun toggle()
    fun seekTo(position: Long)
    fun stop()
    fun skipForward()
    fun skipBackward()
}
