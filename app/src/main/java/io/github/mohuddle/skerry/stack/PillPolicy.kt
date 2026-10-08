package io.github.mohuddle.skerry.stack

/** The pill stays off the camera hole unless there is something live to show. */
fun shouldShowPill(
    islandOn: Boolean,
    overlayAllowed: Boolean,
    listenerConnected: Boolean,
    hasLiveItem: Boolean,
    hasMedia: Boolean = false,
): Boolean {
    return islandOn && overlayAllowed && listenerConnected && (hasLiveItem || hasMedia)
}
