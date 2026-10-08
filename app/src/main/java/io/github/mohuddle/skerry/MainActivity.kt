package io.github.mohuddle.skerry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.mohuddle.skerry.service.SkerryService
import io.github.mohuddle.skerry.ui.SettingsScreen
import io.github.mohuddle.skerry.ui.SkerryTheme
import io.github.mohuddle.skerry.ui.overlayPermissionIntent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var overlayGranted by mutableStateOf(false)
    private var islandOn by mutableStateOf(false)

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startIsland()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            (application as SkerryApp).islandRunning.collect { islandOn = it }
        }
        setContent {
            SkerryTheme {
                SettingsScreen(
                    islandOn = islandOn,
                    onIslandChange = ::onIslandChange,
                    overlayGranted = overlayGranted,
                    onAllowOverlay = { startActivity(overlayPermissionIntent(packageName)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        if ((application as SkerryApp).islandRunning.value && overlayGranted) {
            startIsland()
        }
    }

    private fun onIslandChange(on: Boolean) {
        if (!on) {
            stopService(Intent(this, SkerryService::class.java))
            return
        }
        if (needsNotificationPermission()) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startIsland()
        }
    }

    private fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    }

    private fun startIsland() {
        ContextCompat.startForegroundService(this, Intent(this, SkerryService::class.java))
    }
}
