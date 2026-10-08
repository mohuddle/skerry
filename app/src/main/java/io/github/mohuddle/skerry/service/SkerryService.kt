package io.github.mohuddle.skerry.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.mohuddle.skerry.R
import io.github.mohuddle.skerry.SkerryApp

class SkerryService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = this
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enterForeground(islandNotification(), mediaActive)
        (application as SkerryApp).setIslandRunning(true)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = null
        mediaActive = false
        (application as SkerryApp).setIslandRunning(false)
        super.onDestroy()
    }

    /** Adds mediaPlayback only while an allowlisted session is active. */
    fun setMediaActive(active: Boolean) {
        if (mediaActive == active) return
        mediaActive = active
        enterForeground(islandNotification(), active)
    }

    private fun enterForeground(notification: Notification, withMedia: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification)
            return
        }
        val type = if (withMedia) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        try {
            startForeground(NOTIFICATION_ID, notification, type)
        } catch (error: RuntimeException) {
            if (!withMedia) throw error
            mediaActive = false
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        }
    }

    private fun islandNotification(): Notification {
        val dismiss = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, IslandDismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .setShowWhen(false)
            .setOngoing(false)
            .setDeleteIntent(dismiss)
            .build()
    }

    private var mediaActive = false

    companion object {
        var running: SkerryService? = null
            private set

        private const val CHANNEL_ID = "island"
        private const val NOTIFICATION_ID = 1
    }
}
