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
import platform.MediaPlayer.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*

@OptIn(ExperimentalForeignApi::class)
class IosRadioPlayer : RadioPlayer {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val player = AVPlayer()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var interruptionObserver: Any? = null

    init {
        setupRemoteCommands()
        setupInterruptionObserver()
    }

    private fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionAllowBluetooth or AVAudioSessionCategoryOptionDefaultToSpeaker,
                error = null
            )
            session.setActive(true, error = null)
        } catch (e: Exception) {
            println("Failed to set audio session: ${e.message}")
        }
    }

    private fun setupRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        commandCenter.playCommand.enabled = true
        commandCenter.playCommand.addTargetWithHandler {
            resume()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.pauseCommand.enabled = true
        commandCenter.pauseCommand.addTargetWithHandler {
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.togglePlayPauseCommand.enabled = true
        commandCenter.togglePlayPauseCommand.addTargetWithHandler {
            toggle()
            MPRemoteCommandHandlerStatusSuccess
        }
        
        commandCenter.nextTrackCommand.enabled = true
        commandCenter.nextTrackCommand.addTargetWithHandler {
            skipForward()
            MPRemoteCommandHandlerStatusSuccess
        }
        
        commandCenter.previousTrackCommand.enabled = true
        commandCenter.previousTrackCommand.addTargetWithHandler {
            skipBackward()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun setupInterruptionObserver() {
        interruptionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            val userInfo = notification?.userInfo ?: return@addObserverForName
            val typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber ?: return@addObserverForName

            if (typeValue.unsignedLongValue == AVAudioSessionInterruptionTypeBegan) {
                pause()
            } else if (typeValue.unsignedLongValue == AVAudioSessionInterruptionTypeEnded) {
                val optionsValue = userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber
                if (optionsValue?.unsignedLongValue == AVAudioSessionInterruptionOptionShouldResume) {
                    resume()
                }
            }
        }
    }

    override fun play(station: NetworkRadioStation) {
        configureAudioSession()
        _state.update { it.copy(currentStation = station, error = null, isLoading = true) }
        
        val url = NSURL.URLWithString(station.urlResolved) ?: return
        val playerItem = AVPlayerItem.playerItemWithURL(url)
        
        player.replaceCurrentItemWithPlayerItem(playerItem)
        player.play()

        updateNowPlaying(station, isPlaying = true)

        _state.update { it.copy(isPlaying = true, isLoading = false, isSeekable = false) }
        startProgressUpdate()
    }

    private fun updateNowPlaying(station: NetworkRadioStation, isPlaying: Boolean) {
        val info = mutableMapOf<String, Any>()
        info[MPMediaItemPropertyTitle] = station.name
        info[MPMediaItemPropertyArtist] = station.tags ?: ""
        info[MPNowPlayingInfoPropertyIsLiveStream] = true
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = 0.0 // Reset for live
        
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info as Map<Any?, *>
    }

    override fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
        _state.value.currentStation?.let { updateNowPlaying(it, isPlaying = false) }
        stopProgressUpdate()
    }

    override fun resume() {
        configureAudioSession()
        player.play()
        _state.update { it.copy(isPlaying = true) }
        _state.value.currentStation?.let { updateNowPlaying(it, isPlaying = true) }
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
        interruptionObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _state.update { PlaybackState() }
    }

    override fun close() {
        release()
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
