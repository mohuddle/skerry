package io.github.mohuddle.skerry

import android.app.Application
import io.github.mohuddle.skerry.overlay.SkerryOverlay

class SkerryApp : Application() {
    lateinit var overlay: SkerryOverlay
        private set

    override fun onCreate() {
        super.onCreate()
        overlay = SkerryOverlay(this)
    }
}
