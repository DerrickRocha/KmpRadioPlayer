package com.radio.agilesouthwest.kmpradioplayer.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
class AndroidRadioPlayer(private val context: Context) : RadioPlayer {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                setupControllerListener(controller)
                // Sync initial state
                updatePlaybackState(controller)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to connect to playback service: ${e.message}") }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState(controller)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // If the system (or another controller) changes the track, update our internal state
                mediaItem?.mediaMetadata?.let { metadata ->
                    // Note: We don't have the full NetworkRadioStation object here easily,
                    // but we can update the title/tags at least if needed.
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(error = error.message) }
            }
        })
    }

    private fun updatePlaybackState(player: Player) {
        _state.update { 
            it.copy(
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                duration = if (player.duration > 0) player.duration else 0L,
                isSeekable = !player.isCurrentMediaItemLive || player.isCurrentMediaItemSeekable
            ) 
        }
    }

    override fun play(station: NetworkRadioStation) {
        val controller = this.controller
        if (controller == null) {
            _state.update { it.copy(error = "Player not ready") }
            return
        }

        _state.update { it.copy(currentStation = station, error = null) }
        
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.tags)
            .setArtworkUri(station.favicon?.let { Uri.parse(it) })
            .build()
            
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.stationUuid)
            .setUri(station.urlResolved)
            .setMediaMetadata(metadata)
            .build()
            
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun resume() {
        controller?.play()
    }

    override fun toggle() {
        val controller = this.controller ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    override fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    override fun stop() {
        controller?.stop()
        stopProgressUpdate()
    }

    override fun skipForward() {
        val controller = this.controller ?: return
        controller.seekTo(controller.currentPosition + 5000)
    }

    override fun skipBackward() {
        val controller = this.controller ?: return
        controller.seekTo(controller.currentPosition - 5000)
    }

    override fun release() {
        stopProgressUpdate()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        _state.update { PlaybackState() }
    }

    override fun close() {
        release()
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                controller?.let {
                    _state.update { state -> state.copy(currentPosition = it.currentPosition) }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
