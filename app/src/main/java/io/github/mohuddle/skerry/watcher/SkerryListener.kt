package io.github.mohuddle.skerry.watcher

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.mohuddle.skerry.SkerryApp

class SkerryListener : NotificationListenerService() {
    private var sessions: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var mediaManager: MediaSessionManager? = null

    override fun onListenerConnected() {
        val app = application as SkerryApp
        val component = ComponentName(this, SkerryListener::class.java)
        val manager = getSystemService(MediaSessionManager::class.java)
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            app.onSessions(controllers.orEmpty())
        }
        sessions = listener
        mediaManager = manager
        try {
            manager.addOnActiveSessionsChangedListener(listener, component)
            app.onSessions(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            app.onSessions(emptyList())
        }
        app.attachListener(this)
    }

    override fun onListenerDisconnected() {
        sessions?.let { listener ->
            mediaManager?.removeOnActiveSessionsChangedListener(listener)
        }
        sessions = null
        mediaManager = null
        (application as SkerryApp).detachListener(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        (application as SkerryApp).onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        (application as SkerryApp).onNotificationRemoved(sbn.key)
    }

    fun currentNotifications(): List<StatusBarNotification> {
        return try {
            activeNotifications?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
