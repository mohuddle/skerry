package io.github.mohuddle.skerry.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

data class PillModel(
    val visible: Boolean,
    val expanded: Boolean,
    val title: String,
    val body: String,
    val actions: List<String>,
    val showReply: Boolean,
    val showMedia: Boolean,
    val mediaPlaying: Boolean,
)

interface PillClicks {
    fun cycle()
    fun center()
    fun media()
    fun body()
    fun action(index: Int)
    fun reply(text: String)
    fun replyFocus(focused: Boolean)
}

/** Collapsed row and the expanded title, body, actions, and reply field. */
class PillChrome(context: Context) : LinearLayout(context) {
    private val density = resources.displayMetrics.density
    private val cycle = label("●", "Cycle")
    private val title = label("", "Expand")
    private val media = label("▶", "Play")
    private val body = label("", "Open")
    private val actionRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        visibility = GONE
    }
    private val replyInput = EditText(context).apply {
        hint = "Reply"
        contentDescription = "Reply"
        setSingleLine(true)
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0x99FFFFFF.toInt())
        setBackgroundColor(0x00000000)
        imeOptions = EditorInfo.IME_ACTION_SEND
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    }
    private val send = label("Send", "Send")
    private val replyRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        visibility = GONE
        addView(replyInput)
        addView(send)
    }
    private var expanded = false
    private var actionLabels: List<String> = emptyList()
    private var downY = 0f
    var clicks: PillClicks? = null

    init {
        orientation = VERTICAL
        setTopInset(0)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xF0141414.toInt())
            setStroke(dp(1).coerceAtLeast(1), 0x66FFFFFF)
        }
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(cycle, LayoutParams(dp(28), LayoutParams.WRAP_CONTENT))
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(media, LayoutParams(dp(36), LayoutParams.WRAP_CONTENT))
        }
        addView(row)
        addView(body)
        addView(actionRow)
        addView(replyRow)
        cycle.setOnClickListener { clicks?.cycle() }
        title.setOnClickListener { clicks?.center() }
        media.setOnClickListener { clicks?.media() }
        body.setOnClickListener { clicks?.body() }
        send.setOnClickListener { submitReply() }
        replyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitReply()
                true
            } else {
                false
            }
        }
        replyInput.setOnFocusChangeListener { _, focused -> clicks?.replyFocus(focused) }
    }

    /** Empty space above the controls. That band sits under the status bar, which takes the touches. */
    fun setTopInset(px: Int) {
        val padX = dp(12)
        val padY = dp(4)
        setPadding(padX, padY + px, padX, padY)
    }

    fun bind(model: PillModel) {
        expanded = model.expanded
        title.text = model.title
        title.contentDescription = if (model.expanded) "Collapse" else "Expand"
        body.text = model.body
        body.visibility = if (model.expanded && model.body.isNotBlank()) VISIBLE else GONE
        media.visibility = if (model.showMedia) VISIBLE else GONE
        media.text = if (model.mediaPlaying) "❚❚" else "▶"
        media.contentDescription = if (model.mediaPlaying) "Pause" else "Play"
        replyRow.visibility = if (model.showReply) VISIBLE else GONE
        if (!model.showReply) replyInput.clearFocus()
        if (actionLabels != model.actions) {
            actionLabels = model.actions
            actionRow.removeAllViews()
            model.actions.forEachIndexed { index, label ->
                val view = label(label, label)
                view.setOnClickListener { clicks?.action(index) }
                actionRow.addView(view)
            }
        }
        actionRow.visibility = if (model.expanded && model.actions.isNotEmpty()) VISIBLE else GONE
        (background as GradientDrawable).cornerRadius = if (model.expanded) dp(20).toFloat() else dp(18).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> downY = event.y
            MotionEvent.ACTION_UP -> {
                if (expanded && downY - event.y > dp(36)) clicks?.center()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun submitReply() {
        val text = replyInput.text?.toString().orEmpty()
        if (text.isBlank()) return
        clicks?.reply(text)
        replyInput.setText("")
    }

    private fun label(text: String, description: String): TextView {
        return TextView(context).apply {
            this.text = text
            contentDescription = description
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
    }

    private fun dp(value: Int): Int = (value * density).roundToInt().coerceAtLeast(1)
}
