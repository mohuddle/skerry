package io.github.mohuddle.skerry.watcher

import android.app.Notification
import android.service.notification.StatusBarNotification
import io.github.mohuddle.skerry.stack.LiveItem

/**
 * Package and channel are checked before the title or text is read.
 * A rejected notification is not returned, so its payload is not kept.
 */
fun liveItemFrom(
    sbn: StatusBarNotification,
    allowed: Set<String>,
    selfPackage: String,
): LiveItem? {
    if (sbn.packageName !in allowed) return null
    if (sbn.packageName == selfPackage && sbn.notification.channelId == ISLAND_CHANNEL_ID) return null
    val notification = sbn.notification
    if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
    val extras = notification.extras
    return toLiveItem(
        PostedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            channelId = notification.channelId,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            postedAt = sbn.postTime,
            actionLabels = notification.actions?.map { it.title?.toString().orEmpty() }.orEmpty(),
            hasReply = notification.actions?.any { !it.remoteInputs.isNullOrEmpty() } == true,
        ),
        allowed,
        selfPackage,
    )
}
