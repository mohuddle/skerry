package io.github.mohuddle.skerry.debug

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

/** A local session so play/pause can be checked without another player's library. */
object DebugMedia {
    private var session: MediaSession? = null
    var playing: Boolean = false
        private set

    fun toggle(context: Context) {
        val existing = session
        if (existing == null) {
            playing = true
            val created = MediaSession(context, "skerry-debug")
            created.setCallback(object : MediaSession.Callback() {
                override fun onPlay() = apply(created, true)
                override fun onPause() = apply(created, false)
            })
            @Suppress("DEPRECATION")
            created.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            created.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "Debug track")
                    .build(),
            )
            created.setPlaybackState(state(playing))
            created.isActive = true
            session = created
            return
        }
        apply(existing, !playing)
    }

    private fun apply(session: MediaSession, next: Boolean) {
        playing = next
        session.setPlaybackState(state(next))
        session.isActive = true
    }

    fun release() {
        session?.release()
        session = null
        playing = false
    }

    private fun state(playing: Boolean): PlaybackState {
        val value = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        return PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
            .setState(value, 0L, if (playing) 1f else 0f)
            .build()
    }
}
