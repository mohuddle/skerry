package io.github.mohuddle.skerry.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.DisplayCutout
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

private const val PILL_WIDTH_DP = 128
private const val PILL_HEIGHT_DP = 36

/**
 * Static pill. [io.github.mohuddle.skerry.service.SkerryService] calls [show]
 * from onStart and [hide] from onDestroy.
 */
class SkerryOverlay(context: Context) : ComponentCallbacks {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun show() {
        if (!Settings.canDrawOverlays(appContext)) {
            hide()
            return
        }
        val params = layoutParams()
        val existing = view
        if (existing != null) {
            windowManager.updateViewLayout(existing, params)
            return
        }
        val pill = PillView(appContext)
        try {
            windowManager.addView(pill, params)
        } catch (_: SecurityException) {
            return
        } catch (_: WindowManager.BadTokenException) {
            return
        }
        view = pill
        appContext.registerComponentCallbacks(this)
    }

    fun hide() {
        val pill = view ?: return
        view = null
        appContext.unregisterComponentCallbacks(this)
        try {
            windowManager.removeView(pill)
        } catch (_: IllegalArgumentException) {
            // The window was already gone.
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (view != null) show()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLowMemory() = Unit

    private fun layoutParams(): WindowManager.LayoutParams {
        val density = appContext.resources.displayMetrics.density
        val pillWidth = (PILL_WIDTH_DP * density).roundToInt()
        val pillHeight = (PILL_HEIGHT_DP * density).roundToInt()
        val placement = placePillAtCutout(
            cutouts = cutoutRects(),
            screenWidthPx = screenWidthPx(),
            pillWidthPx = pillWidth,
            pillHeightPx = pillHeight,
            statusBarHeightPx = statusBarHeightPx(),
        )
        return WindowManager.LayoutParams(
            pillWidth,
            pillHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = placement.x
            y = placement.y
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                // Raw screen coordinates. Otherwise the status bar pushes the pill below the hole.
                fitInsetsTypes = 0
            } else {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun cutoutRects(): List<PxRect> {
        val cutout = readDisplayCutout() ?: return emptyList()
        return cutout.boundingRects.map { PxRect(it.left, it.top, it.right, it.bottom) }
    }

    private fun readDisplayCutout(): DisplayCutout? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fromMetrics = windowManager.currentWindowMetrics.windowInsets.displayCutout
            if (fromMetrics != null && fromMetrics.hasHole()) return fromMetrics
        }
        @Suppress("DEPRECATION")
        return windowManager.defaultDisplay.cutout
    }

    private fun screenWidthPx(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.width
        }
    }

    private fun statusBarHeightPx(): Int {
        val id = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id == 0) return 0
        return appContext.resources.getDimensionPixelSize(id)
    }
}

private fun DisplayCutout.hasHole(): Boolean {
    return boundingRects.any { it.width() > 0 && it.height() > 0 }
}

private class PillView(context: Context) : View(context) {
    private val shape = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(0xF0141414.toInt())
        setStroke((resources.displayMetrics.density).roundToInt().coerceAtLeast(1), 0x66FFFFFF)
    }

    init {
        background = shape
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        shape.cornerRadius = h / 2f
    }
}
