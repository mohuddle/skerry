package io.github.mohuddle.skerry

import android.app.Application
import io.github.mohuddle.skerry.allowlist.AllowlistStore
import io.github.mohuddle.skerry.allowlist.createAllowlistStore
import io.github.mohuddle.skerry.overlay.SkerryOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SkerryApp : Application() {
    lateinit var overlay: SkerryOverlay
        private set

    lateinit var allowlist: AllowlistStore
        private set

    private val _islandRunning = MutableStateFlow(false)
    val islandRunning: StateFlow<Boolean> = _islandRunning

    override fun onCreate() {
        super.onCreate()
        overlay = SkerryOverlay(this)
        allowlist = createAllowlistStore(this)
    }

    fun setIslandRunning(running: Boolean) {
        _islandRunning.value = running
    }
}
