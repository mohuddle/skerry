package io.github.mohuddle.skerry.watcher

import io.github.mohuddle.skerry.stack.ItemKind
import io.github.mohuddle.skerry.stack.LiveItem

const val ISLAND_CHANNEL_ID = "island"
const val CHARGING_KEY = "skerry:charging"

/** Fields copied from a status bar notification. The gate decides whether they are kept. */
data class PostedNotification(
    val key: String,
    val packageName: String,
    val channelId: String?,
    val title: String,
    val text: String,
    val groupSummary: Boolean,
    val postedAt: Long,
    val actionLabels: List<String> = emptyList(),
    val hasReply: Boolean = false,
)

fun toLiveItem(
    posted: PostedNotification,
    allowed: Set<String>,
    selfPackage: String,
): LiveItem? {
    if (posted.packageName == selfPackage && posted.channelId == ISLAND_CHANNEL_ID) return null
    if (posted.packageName !in allowed) return null
    if (posted.groupSummary) return null
    if (posted.title.isBlank() && posted.text.isBlank()) return null
    return LiveItem(
        key = posted.key,
        packageName = posted.packageName,
        title = posted.title,
        text = posted.text,
        postedAt = posted.postedAt,
        kind = ItemKind.NOTIFICATION,
        actionLabels = posted.actionLabels,
        hasReply = posted.hasReply,
    )
}

data class MediaSnap(
    val packageName: String,
    val playing: Boolean,
)

fun selectMedia(sessions: List<MediaSnap>, allowed: Set<String>): MediaSnap? {
    return sessions.firstOrNull { it.packageName in allowed }
}

/** Plugged in, including a full battery that is still on power. Status alone stays FULL after unplug on some devices. */
fun batteryPlugged(pluggedExtra: Int): Boolean = pluggedExtra != 0

fun chargingItem(enabled: Boolean, pluggedIn: Boolean, title: String): LiveItem? {
    if (!enabled || !pluggedIn) return null
    return LiveItem(
        key = CHARGING_KEY,
        packageName = null,
        title = title,
        text = "",
        postedAt = 0L,
        kind = ItemKind.CHARGING,
    )
}

fun showsReplyField(item: LiveItem?): Boolean {
    return item != null && item.kind == ItemKind.NOTIFICATION && item.hasReply
}
