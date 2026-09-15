package com.radio.agilesouthwest.kmpradioplayer.media

import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import kotlinx.cinterop.BetaInteropApi
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
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import platform.AVFAudio.*

// NOTE: import list depends on your KMP module's cinterop setup (AVFoundation, MediaPlayer,
// AVFAudio/AVAudioSession, Foundation, kotlinx.cinterop). Add the ones matching your project;
// omitted here for brevity since they vary slightly by Kotlin/Native version.

@OptIn(ExperimentalForeignApi::class)
class IosRadioPlayer : RadioPlayer {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val player = AVPlayer()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Notification observer tokens — all must be removed in release(), or NSNotificationCenter
    // (a process-wide singleton) keeps this instance alive indefinitely.
    private var interruptionObserver: Any? = null
    private var routeChangeObserver: Any? = null
    private var itemFailedObserver: Any? = null
    private var itemStalledObserver: Any? = null

    // Remote command tokens — MPRemoteCommandCenter is also a process-wide singleton.
    private var playCommandTarget: Any? = null
    private var pauseCommandTarget: Any? = null
    private var toggleCommandTarget: Any? = null
    private var nextTrackCommandTarget: Any? = null
    private var previousTrackCommandTarget: Any? = null

    init {
        setupRemoteCommands()
        setupInterruptionObserver()
        setupRouteChangeObserver()
        setupPlaybackFailureObservers()
    }

    // ---------------------------------------------------------------------
    // Audio session
    // ---------------------------------------------------------------------

    @OptIn(BetaInteropApi::class)
    private fun configureAudioSession(): Boolean {
        val session = AVAudioSession.sharedInstance()
        return memScoped {
            val categoryError = alloc<ObjCObjectVar<NSError?>>()
            val categoryOk = session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionAllowBluetooth or
                        AVAudioSessionCategoryOptionAllowBluetoothA2DP or
                        AVAudioSessionCategoryOptionDefaultToSpeaker,
                error = categoryError.ptr
            )
            if (!categoryOk) {
                logError("Failed to set audio session category: ${categoryError.value?.localizedDescription}")
                return@memScoped false
            }

            val activeError = alloc<ObjCObjectVar<NSError?>>()
            val activeOk = session.setActive(true, error = activeError.ptr)
            if (!activeOk) {
                logError("Failed to activate audio session: ${activeError.value?.localizedDescription}")
                return@memScoped false
            }
            true
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun deactivateAudioSession() {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val ok = AVAudioSession.sharedInstance().setActive(
                false,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = error.ptr
            )
            if (!ok) {
                logError("Failed to deactivate audio session: ${error.value?.localizedDescription}")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Remote commands
    // ---------------------------------------------------------------------

    private fun setupRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        commandCenter.playCommand.enabled = true
        playCommandTarget = commandCenter.playCommand.addTargetWithHandler {
            resume()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.pauseCommand.enabled = true
        pauseCommandTarget = commandCenter.pauseCommand.addTargetWithHandler {
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.togglePlayPauseCommand.enabled = true
        toggleCommandTarget = commandCenter.togglePlayPauseCommand.addTargetWithHandler {
            toggle()
            MPRemoteCommandHandlerStatusSuccess
        }

        // Radio streams generally aren't seekable, so these behave as "skip" only when
        // isSeekable is true. Consider disabling the commands entirely for pure live radio.
        commandCenter.nextTrackCommand.enabled = true
        nextTrackCommandTarget = commandCenter.nextTrackCommand.addTargetWithHandler {
            skipForward()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.previousTrackCommand.enabled = true
        previousTrackCommandTarget = commandCenter.previousTrackCommand.addTargetWithHandler {
            skipBackward()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun removeRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        playCommandTarget?.let { commandCenter.playCommand.removeTarget(it) }
        pauseCommandTarget?.let { commandCenter.pauseCommand.removeTarget(it) }
        toggleCommandTarget?.let { commandCenter.togglePlayPauseCommand.removeTarget(it) }
        nextTrackCommandTarget?.let { commandCenter.nextTrackCommand.removeTarget(it) }
        previousTrackCommandTarget?.let { commandCenter.previousTrackCommand.removeTarget(it) }
        playCommandTarget = null
        pauseCommandTarget = null
        toggleCommandTarget = null
        nextTrackCommandTarget = null
        previousTrackCommandTarget = null
    }

    // ---------------------------------------------------------------------
    // Interruptions / route changes / playback failures
    // ---------------------------------------------------------------------

    private fun setupInterruptionObserver() {
        interruptionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            val userInfo = notification?.userInfo ?: return@addObserverForName
            val typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber ?: return@addObserverForName

            when (typeValue.unsignedLongValue) {
                AVAudioSessionInterruptionTypeBegan -> pause()
                AVAudioSessionInterruptionTypeEnded -> {
                    val options = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                        ?.unsignedLongValue ?: 0uL
                    // Bitmask, not a single value — must be checked with AND, not equality.
                    if (options and AVAudioSessionInterruptionOptionShouldResume != 0uL) {
                        resume()
                    }
                }
            }
        }
    }

    private fun setupRouteChangeObserver() {
        routeChangeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            val userInfo = notification?.userInfo ?: return@addObserverForName
            val reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? NSNumber ?: return@addObserverForName

            // e.g. headphones unplugged — pause rather than blast audio out of the speaker.
            if (reasonValue.unsignedLongValue == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                pause()
            }
        }
    }

    private fun setupPlaybackFailureObservers() {
        itemFailedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            if (notification?.`object` !== player.currentItem) return@addObserverForName
            val error = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
            handlePlaybackFailure(error?.localizedDescription ?: "Playback failed")
        }

        itemStalledObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemPlaybackStalledNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            if (notification?.`object` !== player.currentItem) return@addObserverForName
            // Stream stalled (e.g. buffering underrun). Reflect it in state; caller can decide
            // whether to retry, show a spinner, etc.
            _state.update { it.copy(isLoading = true) }
        }
    }

    private fun handlePlaybackFailure(message: String) {
        stopProgressUpdate()
        _state.update { it.copy(isPlaying = false, isLoading = false, error = message) }
    }

    private fun removeAllObservers() {
        val center = NSNotificationCenter.defaultCenter
        interruptionObserver?.let { center.removeObserver(it) }
        routeChangeObserver?.let { center.removeObserver(it) }
        itemFailedObserver?.let { center.removeObserver(it) }
        itemStalledObserver?.let { center.removeObserver(it) }
        interruptionObserver = null
        routeChangeObserver = null
        itemFailedObserver = null
        itemStalledObserver = null
    }

    // ---------------------------------------------------------------------
    // Playback controls
    // ---------------------------------------------------------------------

    override fun play(station: NetworkRadioStation) {
        _state.update { it.copy(currentStation = station, error = null, isLoading = true) }

        val url = NSURL.URLWithString(station.urlResolved)
        if (url == null) {
            _state.update { it.copy(isLoading = false, error = "Invalid station URL") }
            return
        }

        if (!configureAudioSession()) {
            _state.update { it.copy(isLoading = false, error = "Could not configure audio session") }
            return
        }

        val playerItem = AVPlayerItem.playerItemWithURL(url)
        player.replaceCurrentItemWithPlayerItem(playerItem)
        player.play()

        updateNowPlaying(station, isPlaying = true)
        _state.update { it.copy(isPlaying = true, isLoading = false, isSeekable = false) }
        startProgressUpdate()
    }

    override fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
        _state.value.currentStation?.let { updateNowPlaying(it, isPlaying = false) }
        stopProgressUpdate()
    }

    override fun resume() {
        if (!configureAudioSession()) {
            _state.update { it.copy(error = "Could not configure audio session") }
            return
        }
        player.play()
        _state.update { it.copy(isPlaying = true, error = null) }
        _state.value.currentStation?.let { updateNowPlaying(it, isPlaying = true) }
        startProgressUpdate()
    }

    override fun toggle() {
        if (_state.value.isPlaying) pause() else resume()
    }

    override fun seekTo(position: Long) {
        if (!_state.value.isSeekable) return
        val cmTime = CMTimeMake(position, 1000)
        player.seekToTime(cmTime)
    }

    override fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        deactivateAudioSession()
        _state.update { PlaybackState() }
        stopProgressUpdate()
    }

    override fun skipForward() {
        if (!_state.value.isSeekable) return
        val current = CMTimeGetSeconds(player.currentTime())
        seekTo(((current + 5) * 1000).toLong())
    }

    override fun skipBackward() {
        if (!_state.value.isSeekable) return
        val current = CMTimeGetSeconds(player.currentTime())
        seekTo(((current - 5) * 1000).toLong())
    }

    override fun release() {
        stopProgressUpdate()
        scope.cancel()
        removeAllObservers()
        removeRemoteCommands()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        deactivateAudioSession()
        _state.update { PlaybackState() }
    }

    override fun close() {
        release()
    }

    // ---------------------------------------------------------------------
    // Now playing / progress
    // ---------------------------------------------------------------------

    private fun updateNowPlaying(station: NetworkRadioStation, isPlaying: Boolean) {
        val info: Map<Any?, Any?> = mapOf(
            MPMediaItemPropertyTitle to station.name,
            MPMediaItemPropertyArtist to (station.tags ?: ""),
            MPNowPlayingInfoPropertyIsLiveStream to true,
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) 1.0 else 0.0,
            MPNowPlayingInfoPropertyElapsedPlaybackTime to 0.0 // reset for live
        )
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
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

    private fun logError(message: String) {
        println("[IosRadioPlayer] $message")
    }
}
