package io.github.mohuddle.skerry.stack

/**
 * Newest notification first. Charging, when present, stays at the end and is
 * not counted against the notification cap until the list is trimmed.
 */
class ActivityStack(private val cap: Int = 5) {
    private var state = StackState()

    fun state(): StackState = state

    fun offer(item: LiveItem) {
        if (item.kind == ItemKind.CHARGING) {
            setCharging(item)
            return
        }
        val charging = chargingItems()
        val notifications = listOf(item) + notificationsExcept(item.key)
        state = state.copy(
            items = trim(notifications, charging),
            currentIndex = 0,
            expanded = state.expanded && state.current?.key == item.key,
        )
    }

    fun remove(key: String) {
        val currentKey = state.current?.key
        val items = state.items.filterNot { it.key == key }
        state = state.copy(
            items = items,
            currentIndex = indexOfKey(items, if (currentKey == key) null else currentKey),
            expanded = state.expanded && items.any { it.key == currentKey && currentKey != key },
        )
    }

    /** Replaces notification rows and leaves a charging row in place. Newest first. */
    fun replaceNotifications(notifications: List<LiveItem>) {
        val currentKey = state.current?.key
        val items = trim(notifications.filter { it.kind != ItemKind.CHARGING }, chargingItems())
        val index = indexOfKey(items, currentKey)
        state = state.copy(
            items = items,
            currentIndex = index,
            expanded = state.expanded && items.getOrNull(index)?.key == currentKey,
        )
    }

    fun cycle() {
        if (state.items.size < 2) return
        val next = (state.currentIndex + 1) % state.items.size
        state = state.copy(currentIndex = next, expanded = false)
    }

    fun toggleExpanded() {
        if (state.current == null) return
        state = state.copy(expanded = !state.expanded)
    }

    fun collapse() {
        state = state.copy(expanded = false)
    }

    fun setCharging(item: LiveItem?) {
        val currentKey = state.current?.key
        val charging = if (item == null) emptyList() else listOf(item.copy(kind = ItemKind.CHARGING))
        val items = trim(notificationsExcept(null), charging)
        state = state.copy(
            items = items,
            currentIndex = indexOfKey(items, currentKey),
            expanded = state.expanded && items.any { it.key == currentKey },
        )
    }

    private fun notificationsExcept(key: String?): List<LiveItem> {
        return state.items.filter { it.kind != ItemKind.CHARGING && it.key != key }
    }

    private fun chargingItems(): List<LiveItem> = state.items.filter { it.kind == ItemKind.CHARGING }

    private fun trim(notifications: List<LiveItem>, charging: List<LiveItem>): List<LiveItem> {
        val room = (cap - charging.size).coerceAtLeast(0)
        return notifications.take(room) + charging
    }

    private fun indexOfKey(items: List<LiveItem>, key: String?): Int {
        if (items.isEmpty() || key == null) return 0
        val index = items.indexOfFirst { it.key == key }
        return if (index < 0) 0 else index
    }
}
