package io.github.mohuddle.skerry.overlay

import kotlin.math.abs

data class PxRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    fun isEmpty(): Boolean = width <= 0 || height <= 0
}

data class PillPlacement(
    val x: Int,
    val y: Int,
)

/**
 * Centers the pill on the highest camera hole. Empty rects are ignored.
 * With no hole, the pill is centered in the status-bar band.
 * [PillPlacement.x] and [PillPlacement.y] are top-left pixels, clamped on screen.
 */
fun placePillAtCutout(
    cutouts: List<PxRect>,
    screenWidthPx: Int,
    pillWidthPx: Int,
    pillHeightPx: Int,
    statusBarHeightPx: Int,
): PillPlacement {
    val hole = cutouts
        .filterNot { it.isEmpty() }
        .minWithOrNull(
            compareBy<PxRect> { it.top }
                .thenBy { abs(it.centerX - screenWidthPx / 2) },
        )
    val rawX: Int
    val rawY: Int
    if (hole == null) {
        rawX = (screenWidthPx - pillWidthPx) / 2
        rawY = (statusBarHeightPx - pillHeightPx) / 2
    } else {
        rawX = hole.centerX - pillWidthPx / 2
        rawY = hole.centerY - pillHeightPx / 2
    }
    val maxX = (screenWidthPx - pillWidthPx).coerceAtLeast(0)
    return PillPlacement(
        x = rawX.coerceIn(0, maxX),
        y = rawY.coerceAtLeast(0),
    )
}

data class PillTouchShift(
    val topInset: Int,
    val windowHeight: Int,
)

/**
 * The status bar is a trusted overlay and takes every touch in its band,
 * including the camera hole. Keep the window top on the hole, and push the
 * controls to the first pixel below that band.
 */
fun shiftContentBelowStatusBar(
    windowTop: Int,
    contentHeight: Int,
    statusBarBottom: Int,
): PillTouchShift {
    val topInset = (statusBarBottom - windowTop).coerceAtLeast(0)
    return PillTouchShift(
        topInset = topInset,
        windowHeight = topInset + contentHeight,
    )
}
