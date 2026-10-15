package com.radio.agilesouthwest.kmpradioplayer.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyHeaders
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radio.agilesouthwest.kmpradioplayer.KEY_ALBUM
import com.radio.agilesouthwest.kmpradioplayer.KEY_ARTIST
import com.radio.agilesouthwest.kmpradioplayer.KEY_TITLE

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(
                        DefaultHttpDataSource.Factory()
                            .setUserAgent("KmpRadioPlayer/1.0")
                            .setAllowCrossProtocolRedirects(true)
                    )
            )
            .build()

        player?.addListener(object : Player.Listener {
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    when (val entry = metadata[i]) {
                        is IcyInfo -> {
                            // e.g. "Artist - Song Title"
                            val streamTitle = entry.title
                            if (streamTitle.isNullOrBlank()) continue
                            val artistSong = streamTitle.split("|")

                            val song = if (artistSong.size == 1) artistSong.first() else if (artistSong.size > 1) artistSong[1] else null
                            val artist = if (artistSong.size > 1) artistSong.first() else null
                            mediaSession?.sessionExtras = buildBundle(artist, song)
                            Log.d("onMetadataIcyInfo", streamTitle)
                        }

                        is IcyHeaders -> {
                            // Station-level info sent when the connection opens
                            val stationName = entry.name
                            val genre = entry.genre
                            mediaSession?.sessionExtras = buildBundle(genre, stationName)
                            Log.d("onMetadataIcyHeaders", "$stationName - $genre")
                        }

                        is TextInformationFrame -> {
                            mediaSession?.sessionExtras = buildTextInformationFrameBundle(entry)
                            Log.d("onMetadataTextInformationFrame", entry.values.toString())
                        }
                    }
                }
            }

            private fun buildTextInformationFrameBundle(entry: TextInformationFrame): Bundle {
                val song = when(entry.id) {
                    "TIT2" -> entry.value
                    else -> "Unknown"
                }
                val artist = when(entry.id) {
                    "TPE2", "TIT1" -> entry.value
                    else -> "Unknown"
                }
                val album = when(entry.id) {
                    "TALB", "TCON" -> entry.value
                    else -> "Unknown"
                }
                return bundleOf(
                    KEY_ARTIST to artist,
                    KEY_TITLE to song,
                    KEY_ALBUM to album
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaSession?.sessionExtras = Bundle.EMPTY // new station, clear old song
            }
        })

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun buildBundle(
        artist: String? = null,
        song: String? = null,
        album: String? = null
    ): Bundle {
        return bundleOf(
            KEY_ARTIST to artist,
            KEY_TITLE to song,
            KEY_ALBUM to album
        )
    }

    /* private fun onNowPlaying(rawTitle: String) {
         val parsed = parseStreamTitle(rawTitle)
         if (parsed != _nowPlaying.value) {   // ignore repeats
             _nowPlaying.value = parsed
             // trigger the lyrics lookup here, or collect nowPlaying elsewhere
         }
     }*/

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
