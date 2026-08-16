package com.radio.agilesouthwest.kmpradioplayer.media

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.Foundation.*
import platform.CoreMedia.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosRadioPlayer : RadioPlayer {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val player = AVPlayer()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    override fun play(station: NetworkRadioStation) {
        _state.update { it.copy(currentStation = station, error = null, isLoading = true) }
        val url = NSURL.URLWithString(station.urlResolved) ?: return
        val playerItem = AVPlayerItem.playerItemWithURL(url)
        
        player.replaceCurrentItemWithPlayerItem(playerItem)
        player.play()
        // Simplified state management for KMP implementation
        _state.update { it.copy(isPlaying = true, isLoading = false, isSeekable = false) }
        startProgressUpdate()
    }

    override fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
        stopProgressUpdate()
    }

    override fun resume() {
        player.play()
        _state.update { it.copy(isPlaying = true) }
        startProgressUpdate()
    }

    override fun toggle() {
        if (_state.value.isPlaying) pause() else resume()
    }

    override fun seekTo(position: Long) {
        val cmTime = CMTimeMake(position, 1000)
        player.seekToTime(cmTime)
    }

    override fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _state.update { PlaybackState() }
        stopProgressUpdate()
    }

    override fun skipForward() {
        val current = CMTimeGetSeconds(player.currentTime())
        seekTo(((current + 5) * 1000).toLong())
    }

    override fun skipBackward() {
        val current = CMTimeGetSeconds(player.currentTime())
        seekTo(((current - 5) * 1000).toLong())
    }

    override fun release() {
        stopProgressUpdate()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _state.update { PlaybackState() }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val current = CMTimeGetSeconds(player.currentTime())
                _state.update { it.copy(currentPosition = (current * 1000).toLong()) }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
