package io.github.mohuddle.skerry.stack

enum class ItemKind {
    NOTIFICATION,
    CHARGING,
}

/** In-memory only. Titles and text are never written to disk. */
data class LiveItem(
    val key: String,
    val packageName: String?,
    val title: String,
    val text: String,
    val postedAt: Long,
    val kind: ItemKind = ItemKind.NOTIFICATION,
    val actionLabels: List<String> = emptyList(),
    val hasReply: Boolean = false,
)

data class StackState(
    val items: List<LiveItem> = emptyList(),
    val currentIndex: Int = 0,
    val expanded: Boolean = false,
) {
    val current: LiveItem? get() = items.getOrNull(currentIndex)
}
