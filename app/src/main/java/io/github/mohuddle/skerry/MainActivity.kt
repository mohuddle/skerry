package io.github.mohuddle.skerry

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mohuddle.skerry.ui.SettingsScreen
import io.github.mohuddle.skerry.ui.SkerryTheme
import io.github.mohuddle.skerry.ui.overlayPermissionIntent

class MainActivity : ComponentActivity() {
    private var overlayGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkerryTheme {
                SettingsScreen(
                    overlayGranted = overlayGranted,
                    onAllowOverlay = { startActivity(overlayPermissionIntent(packageName)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        val overlay = (application as SkerryApp).overlay
        if (overlayGranted) overlay.show() else overlay.hide()
    }
}
