package io.github.mohuddle.skerry.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.DisplayCutout
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import kotlin.math.roundToInt

private const val COLLAPSED_WIDTH_DP = 220
private const val EXPANDED_WIDTH_DP = 300
private const val COLLAPSED_HEIGHT_DP = 40

/**
 * Pill window. The island service and the notification listener call [render].
 * An empty model removes the window so the camera hole stays clear.
 */
class SkerryOverlay(context: Context) : ComponentCallbacks, PillClicks {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val input = appContext.getSystemService(InputMethodManager::class.java)
    private var view: PillChrome? = null
    private var model: PillModel = hiddenModel()
    private var replyFocused = false

    var onCycle: () -> Unit = {}
    var onCenter: () -> Unit = {}
    var onMedia: () -> Unit = {}
    var onBody: () -> Unit = {}
    var onAction: (Int) -> Unit = {}
    var onReply: (String) -> Unit = {}

    fun render(next: PillModel) {
        model = next
        if (!next.visible || !Settings.canDrawOverlays(appContext)) {
            hide()
            return
        }
        val pill = ensureView()
        pill.bind(next)
        if (!next.showReply) replyFocused = false
        val width = dp(if (next.expanded) EXPANDED_WIDTH_DP else COLLAPSED_WIDTH_DP)
        pill.setTopInset(0)
        val contentHeight = if (next.expanded) measuredHeight(pill, width) else dp(COLLAPSED_HEIGHT_DP)
        val placement = placePillAtCutout(
            cutouts = cutoutRects(),
            screenWidthPx = screenWidthPx(),
            pillWidthPx = width,
            pillHeightPx = contentHeight,
            statusBarHeightPx = statusBarHeightPx(),
        )
        val shift = shiftContentBelowStatusBar(placement.y, contentHeight, statusBarBottom())
        pill.setTopInset(shift.topInset)
        val params = layoutParams(width, shift.windowHeight, placement, replyFocused)
        try {
            if (view == null) {
                windowManager.addView(pill, params)
                view = pill
                appContext.registerComponentCallbacks(this)
            } else {
                windowManager.updateViewLayout(pill, params)
            }
        } catch (_: SecurityException) {
            dropView()
        } catch (_: WindowManager.BadTokenException) {
            dropView()
        } catch (_: IllegalArgumentException) {
            dropView()
        }
    }

    fun hide() {
        model = hiddenModel()
        replyFocused = false
        val pill = view ?: return
        view = null
        appContext.unregisterComponentCallbacks(this)
        try {
            windowManager.removeView(pill)
        } catch (_: IllegalArgumentException) {
            // The window was already gone.
        }
    }

    override fun cycle() = onCycle()

    override fun center() = onCenter()

    override fun media() = onMedia()

    override fun body() = onBody()

    override fun action(index: Int) = onAction(index)

    override fun reply(text: String) = onReply(text)

    override fun replyFocus(focused: Boolean) {
        if (replyFocused == focused) return
        replyFocused = focused
        if (view != null && model.visible) render(model)
        if (!focused) input.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (view != null && model.visible) render(model)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLowMemory() = Unit

    private fun ensureView(): PillChrome {
        return view ?: PillChrome(appContext).also { it.clicks = this }
    }

    private fun dropView() {
        view = null
        try {
            appContext.unregisterComponentCallbacks(this)
        } catch (_: IllegalArgumentException) {
            // Not registered.
        }
    }

    private fun measuredHeight(pill: View, width: Int): Int {
        pill.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        return pill.measuredHeight.coerceAtLeast(dp(COLLAPSED_HEIGHT_DP))
    }

    private fun layoutParams(
        width: Int,
        height: Int,
        placement: PillPlacement,
        focusable: Boolean,
    ): WindowManager.LayoutParams {
        val focusFlag = if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            focusFlag or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = placement.x
            y = placement.y
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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

    private fun statusBarBottom(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())
                .top
        }
        return statusBarHeightPx()
    }

    private fun statusBarHeightPx(): Int {
        val id = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id == 0) return 0
        return appContext.resources.getDimensionPixelSize(id)
    }

    private fun dp(value: Int): Int {
        return (value * appContext.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    }
}

private fun hiddenModel() = PillModel(
    visible = false,
    expanded = false,
    title = "",
    body = "",
    actions = emptyList(),
    showReply = false,
    showMedia = false,
    mediaPlaying = false,
)

private fun DisplayCutout.hasHole(): Boolean {
    return boundingRects.any { it.width() > 0 && it.height() > 0 }
}
