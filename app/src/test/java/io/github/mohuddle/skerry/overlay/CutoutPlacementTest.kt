package io.github.mohuddle.skerry.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class CutoutPlacementTest {

    @Test
    fun centersThePillOnAPunchHole() {
        // Pixel 9 Pro top cutout. Center is (640, 102). A 384×108 pill lands at (448, 48).
        // A status-bar fallback would put y at 16, so this fails if the hole is ignored.
        val placement = placePillAtCutout(
            cutouts = listOf(PxRect(586, 0, 695, 204)),
            screenWidthPx = 1280,
            pillWidthPx = 384,
            pillHeightPx = 108,
            statusBarHeightPx = 140,
        )

        assertEquals(448, placement.x)
        assertEquals(48, placement.y)
    }

    @Test
    fun ignoresEmptyCutoutRectsAndUsesTheRealHole() {
        val placement = placePillAtCutout(
            cutouts = listOf(
                PxRect(0, 0, 0, 0),
                PxRect(586, 0, 695, 204),
                PxRect(0, 0, 0, 0),
            ),
            screenWidthPx = 1280,
            pillWidthPx = 384,
            pillHeightPx = 108,
            statusBarHeightPx = 140,
        )

        assertEquals(448, placement.x)
        assertEquals(48, placement.y)
    }

    @Test
    fun clampsALeftEdgeHoleOntoTheScreen() {
        // Hole center (20, 30), pill 200×40. Unclamped x is -80.
        val placement = placePillAtCutout(
            cutouts = listOf(PxRect(0, 10, 40, 50)),
            screenWidthPx = 1080,
            pillWidthPx = 200,
            pillHeightPx = 40,
            statusBarHeightPx = 80,
        )

        assertEquals(0, placement.x)
        assertEquals(10, placement.y)
    }

    @Test
    fun clampsARightEdgeHoleOntoTheScreen() {
        // Hole center (1060, 20), pill 200×40. Unclamped x is 960; the last on-screen x is 880.
        val placement = placePillAtCutout(
            cutouts = listOf(PxRect(1040, 0, 1080, 40)),
            screenWidthPx = 1080,
            pillWidthPx = 200,
            pillHeightPx = 40,
            statusBarHeightPx = 80,
        )

        assertEquals(880, placement.x)
        assertEquals(0, placement.y)
    }

    @Test
    fun centersInTheStatusBarWhenTheDisplayHasNoCutout() {
        val placement = placePillAtCutout(
            cutouts = emptyList(),
            screenWidthPx = 1080,
            pillWidthPx = 200,
            pillHeightPx = 36,
            statusBarHeightPx = 80,
        )

        assertEquals(440, placement.x)
        assertEquals(22, placement.y)
    }

    @Test
    fun pinsTheFallbackPillToTheTopWhenItIsTallerThanTheStatusBar() {
        val placement = placePillAtCutout(
            cutouts = listOf(PxRect(0, 0, 0, 0)),
            screenWidthPx = 1080,
            pillWidthPx = 200,
            pillHeightPx = 36,
            statusBarHeightPx = 20,
        )

        assertEquals(440, placement.x)
        assertEquals(0, placement.y)
    }

    @Test
    fun prefersTheHighestHoleWhenSeveralCutoutsExist() {
        // Top-center hole center (540, 40) beats the lower-left hole.
        val placement = placePillAtCutout(
            cutouts = listOf(
                PxRect(0, 40, 40, 80),
                PxRect(500, 0, 580, 80),
            ),
            screenWidthPx = 1080,
            pillWidthPx = 100,
            pillHeightPx = 20,
            statusBarHeightPx = 80,
        )

        assertEquals(490, placement.x)
        assertEquals(30, placement.y)
    }

    @Test
    fun breaksATieByChoosingTheHoleCloserToHorizontalCenter() {
        // Both tops are 0. Screen center is 500. The center hole wins over the left edge.
        val placement = placePillAtCutout(
            cutouts = listOf(
                PxRect(0, 0, 40, 40),
                PxRect(480, 0, 520, 40),
            ),
            screenWidthPx = 1000,
            pillWidthPx = 40,
            pillHeightPx = 20,
            statusBarHeightPx = 40,
        )

        assertEquals(480, placement.x)
        assertEquals(10, placement.y)
    }

    @Test
    fun keepsATallPillFromSittingAboveTheScreen() {
        // Hole center (120, 5), pill height 40. Unclamped y is -15.
        val placement = placePillAtCutout(
            cutouts = listOf(PxRect(100, 0, 140, 10)),
            screenWidthPx = 400,
            pillWidthPx = 40,
            pillHeightPx = 40,
            statusBarHeightPx = 24,
        )

        assertEquals(100, placement.x)
        assertEquals(0, placement.y)
    }

    @Test
    fun pinsAPillWiderThanTheScreenToTheLeftEdge() {
        val placement = placePillAtCutout(
            cutouts = emptyList(),
            screenWidthPx = 100,
            pillWidthPx = 180,
            pillHeightPx = 20,
            statusBarHeightPx = 40,
        )

        assertEquals(0, placement.x)
        assertEquals(10, placement.y)
    }
}
