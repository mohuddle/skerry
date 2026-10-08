package io.github.mohuddle.skerry.watcher

import android.media.session.MediaController
import android.media.session.PlaybackState

/** Picks the first allowlisted session and follows its play/pause state. */
class MediaHub(private val onChange: () -> Unit) {
    private val callbacks = mutableListOf<Pair<MediaController, MediaController.Callback>>()
    private var controllers: List<MediaController> = emptyList()
    private var allowed: Set<String> = emptySet()
    var snap: MediaSnap? = null
        private set
    private var current: MediaController? = null

    fun update(next: List<MediaController>, allowed: Set<String>) {
        this.allowed = allowed
        clearCallbacks()
        controllers = next
        next.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            }
            controller.registerCallback(callback)
            callbacks += controller to callback
        }
        publish()
    }

    fun clear() {
        clearCallbacks()
        controllers = emptyList()
        snap = null
        current = null
    }

    fun toggle() {
        val controller = current ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    private fun publish() {
        val chosen = selectMedia(
            controllers.map { MediaSnap(it.packageName, it.playbackState?.state == PlaybackState.STATE_PLAYING) },
            allowed,
        )
        snap = chosen
        current = controllers.firstOrNull { it.packageName == chosen?.packageName }
        onChange()
    }

    private fun clearCallbacks() {
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks.clear()
    }
}
