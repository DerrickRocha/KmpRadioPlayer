package com.radio.agilesouthwest.kmpradioplayer.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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

@OptIn(UnstableApi::class)
class AndroidRadioPlayer(context: Context) : RadioPlayer {
    private val exoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setUserAgent("KmpRadioPlayer/1.0")
                        .setAllowCrossProtocolRedirects(true)
                )
        )
        .build()
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update { 
                    it.copy(
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L,
                        isSeekable = !exoPlayer.isCurrentMediaItemLive || exoPlayer.isCurrentMediaItemSeekable
                    ) 
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(error = error.message) }
            }
        })
    }

    override fun play(station: NetworkRadioStation) {
        _state.update { it.copy(currentStation = station, error = null) }
        val mediaItem = MediaItem.fromUri(station.urlResolved)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun resume() {
        exoPlayer.play()
    }

    override fun toggle() {
        if (exoPlayer.isPlaying) pause() else resume()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun stop() {
        exoPlayer.stop()
        stopProgressUpdate()
    }

    override fun skipForward() {
        exoPlayer.seekTo(exoPlayer.currentPosition + 5000)
    }

    override fun skipBackward() {
        exoPlayer.seekTo(exoPlayer.currentPosition - 5000)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _state.update { it.copy(currentPosition = exoPlayer.currentPosition) }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
