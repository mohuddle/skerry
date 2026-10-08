package io.github.mohuddle.skerry.watcher

import io.github.mohuddle.skerry.stack.ItemKind
import io.github.mohuddle.skerry.stack.shouldShowPill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGateTest {
    private val self = "io.github.mohuddle.skerry"
    private val allowed = setOf("app.allowed", self)

    @Test
    fun keepsAnAllowedNotificationAndDropsTheOtherPackage() {
        val kept = listOf(
            posted("app.allowed", "Hello"),
            posted("app.other", "Secret"),
        ).mapNotNull { toLiveItem(it, allowed, self) }

        assertEquals(listOf("Hello"), kept.map { it.title })
        assertTrue(kept.none { it.text == "Secret" || it.packageName == "app.other" })
    }

    @Test
    fun dropsTheIslandNoticeGroupSummariesAndBlankPayloads() {
        assertNull(toLiveItem(posted(self, "Skerry is on", channelId = ISLAND_CHANNEL_ID), allowed, self))
        assertNull(toLiveItem(posted("app.allowed", "Bundle", groupSummary = true), allowed, self))
        assertNull(toLiveItem(posted("app.allowed", "  ", text = " "), allowed, self))
    }

    @Test
    fun keepsReplyAndActionLabelsForAnAllowedPackage() {
        val item = toLiveItem(
            posted("app.allowed", "Chat", actionLabels = listOf("Reply"), hasReply = true),
            allowed,
            self,
        )

        assertEquals(listOf("Reply"), item?.actionLabels)
        assertTrue(item?.hasReply == true)
        assertTrue(showsReplyField(item))
        assertFalse(showsReplyField(item?.copy(hasReply = false)))
    }

    @Test
    fun selectsTheFirstAllowlistedMediaSession() {
        val chosen = selectMedia(
            listOf(MediaSnap("app.other", true), MediaSnap("app.allowed", false)),
            allowed,
        )

        assertEquals("app.allowed", chosen?.packageName)
        assertFalse(chosen?.playing == true)
        assertNull(selectMedia(listOf(MediaSnap("app.other", true)), allowed))
    }

    @Test
    fun fullStatusWithoutAPlugIsNotPlugged() {
        assertFalse(batteryPlugged(0))
        assertTrue(batteryPlugged(1))
    }

    @Test
    fun chargingItemRequiresTheToggleAndPower() {
        assertNull(chargingItem(enabled = false, pluggedIn = true, title = "Charging"))
        assertNull(chargingItem(enabled = true, pluggedIn = false, title = "Charging"))
        val item = chargingItem(enabled = true, pluggedIn = true, title = "Charging")
        assertEquals(ItemKind.CHARGING, item?.kind)
        assertEquals(CHARGING_KEY, item?.key)
        assertNull(item?.packageName)
    }

    @Test
    fun pillStaysHiddenUntilTheIslandHasSomethingLive() {
        assertFalse(shouldShowPill(islandOn = true, overlayAllowed = true, listenerConnected = true, hasLiveItem = false))
        assertFalse(shouldShowPill(islandOn = false, overlayAllowed = true, listenerConnected = true, hasLiveItem = true))
        assertFalse(shouldShowPill(islandOn = true, overlayAllowed = false, listenerConnected = true, hasLiveItem = true))
        assertFalse(shouldShowPill(islandOn = true, overlayAllowed = true, listenerConnected = false, hasLiveItem = true))
        assertTrue(shouldShowPill(islandOn = true, overlayAllowed = true, listenerConnected = true, hasLiveItem = true))
        assertTrue(
            shouldShowPill(
                islandOn = true,
                overlayAllowed = true,
                listenerConnected = true,
                hasLiveItem = false,
                hasMedia = true,
            ),
        )
    }

    private fun posted(
        packageName: String,
        title: String,
        text: String = "body",
        channelId: String? = "messages",
        groupSummary: Boolean = false,
        actionLabels: List<String> = emptyList(),
        hasReply: Boolean = false,
    ) = PostedNotification(
        key = "$packageName:$title",
        packageName = packageName,
        channelId = channelId,
        title = title,
        text = text,
        groupSummary = groupSummary,
        postedAt = 1L,
        actionLabels = actionLabels,
        hasReply = hasReply,
    )
}
