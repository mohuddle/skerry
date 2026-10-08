package io.github.mohuddle.skerry.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.mohuddle.skerry.watcher.SkerryListener

fun notificationAccessGranted(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

fun notificationAccessIntent(context: Context): Intent {
    val component = ComponentName(context, SkerryListener::class.java).flattenToString()
    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
        component,
    )
    return if (detail.resolveActivity(context.packageManager) != null) {
        detail
    } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}
