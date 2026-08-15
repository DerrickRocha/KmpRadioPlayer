package com.radio.agilesouthwest.kmpradioplayer.ui.player

import androidx.lifecycle.ViewModel
import com.radio.agilesouthwest.kmpradioplayer.data.network.models.NetworkRadioStation
import com.radio.agilesouthwest.kmpradioplayer.media.RadioPlayer
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(private val player: RadioPlayer) : ViewModel() {
    val playbackState = player.state

    private var stationList: List<NetworkRadioStation> = emptyList()

    fun setStationList(stations: List<NetworkRadioStation>) {
        stationList = stations
    }

    fun playStation(station: NetworkRadioStation) {
        player.play(station)
    }

    fun togglePlayback() {
        player.toggle()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun skipForward() {
        if (playbackState.value.isSeekable) {
            player.skipForward()
        } else {
            playNextStation()
        }
    }

    fun skipBackward() {
        if (playbackState.value.isSeekable) {
            player.skipBackward()
        } else {
            playPreviousStation()
        }
    }

    private fun playNextStation() {
        val current = playbackState.value.currentStation ?: return
        val index = stationList.indexOfFirst { it.stationUuid == current.stationUuid }
        if (index != -1 && index < stationList.size - 1) {
            playStation(stationList[index + 1])
        }
    }

    private fun playPreviousStation() {
        val current = playbackState.value.currentStation ?: return
        val index = stationList.indexOfFirst { it.stationUuid == current.stationUuid }
        if (index > 0) {
            playStation(stationList[index - 1])
        }
    }
}
