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
import io.github.mohuddle.skerry.allowlist.LaunchableApp
import io.github.mohuddle.skerry.allowlist.loadLaunchableApps
import io.github.mohuddle.skerry.service.SkerryService
import io.github.mohuddle.skerry.ui.SettingsScreen
import io.github.mohuddle.skerry.ui.SkerryTheme
import io.github.mohuddle.skerry.ui.batterySettingsIntent
import io.github.mohuddle.skerry.ui.batteryUnrestricted
import io.github.mohuddle.skerry.ui.notificationAccessGranted
import io.github.mohuddle.skerry.ui.notificationAccessIntent
import io.github.mohuddle.skerry.ui.overlayPermissionIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var overlayGranted by mutableStateOf(false)
    private var listenerGranted by mutableStateOf(false)
    private var chargingOn by mutableStateOf(false)
    private var batteryFree by mutableStateOf(false)
    private var islandOn by mutableStateOf(false)
    private var launchableApps by mutableStateOf<List<LaunchableApp>?>(null)
    private var allowedPackages by mutableStateOf<Set<String>>(emptySet())

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startIsland()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SkerryApp
        lifecycleScope.launch {
            app.islandRunning.collect { islandOn = it }
        }
        lifecycleScope.launch {
            app.allowlist.packages.collect { allowedPackages = it }
        }
        lifecycleScope.launch {
            app.charging.enabled.collect { chargingOn = it }
        }
        lifecycleScope.launch {
            launchableApps = withContext(Dispatchers.Default) {
                loadLaunchableApps(packageManager)
            }
        }
        setContent {
            SkerryTheme {
                SettingsScreen(
                    islandOn = islandOn,
                    onIslandChange = ::onIslandChange,
                    overlayGranted = overlayGranted,
                    onAllowOverlay = { startActivity(overlayPermissionIntent(packageName)) },
                    listenerGranted = listenerGranted,
                    onAllowListener = { startActivity(notificationAccessIntent(this)) },
                    chargingOn = chargingOn,
                    onChargingChange = ::onChargingChange,
                    batteryUnrestricted = batteryFree,
                    onBattery = { startActivity(batterySettingsIntent()) },
                    apps = launchableApps,
                    allowedPackages = allowedPackages,
                    onAppChange = ::onAppChange,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        listenerGranted = notificationAccessGranted(this)
        batteryFree = batteryUnrestricted(this)
        val app = application as SkerryApp
        app.refreshPill()
        if (app.islandRunning.value && overlayGranted) {
            startIsland()
        }
    }

    private fun onChargingChange(on: Boolean) {
        val store = (application as SkerryApp).charging
        lifecycleScope.launch { store.setEnabled(on) }
    }

    private fun onAppChange(packageName: String, allowed: Boolean) {
        val store = (application as SkerryApp).allowlist
        lifecycleScope.launch {
            if (allowed) store.add(packageName) else store.remove(packageName)
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

    companion object {
        const val EXTRA_OPENED = "skerry_opened"
    }
}
