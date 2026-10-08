package io.github.mohuddle.skerry

import android.app.Application
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaController
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import io.github.mohuddle.skerry.allowlist.AllowlistStore
import io.github.mohuddle.skerry.allowlist.ChargingPreference
import io.github.mohuddle.skerry.allowlist.createSkerryStores
import io.github.mohuddle.skerry.overlay.PillModel
import io.github.mohuddle.skerry.overlay.SkerryOverlay
import io.github.mohuddle.skerry.service.SkerryService
import io.github.mohuddle.skerry.stack.ActivityStack
import io.github.mohuddle.skerry.stack.shouldShowPill
import io.github.mohuddle.skerry.watcher.MediaHub
import io.github.mohuddle.skerry.watcher.NotificationActions
import io.github.mohuddle.skerry.watcher.SkerryListener
import io.github.mohuddle.skerry.watcher.batteryPlugged
import io.github.mohuddle.skerry.watcher.chargingItem
import io.github.mohuddle.skerry.watcher.liveItemFrom
import io.github.mohuddle.skerry.watcher.showsReplyField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SkerryApp : Application() {
    lateinit var overlay: SkerryOverlay
        private set

    lateinit var allowlist: AllowlistStore
        private set

    lateinit var charging: ChargingPreference
        private set

    val stack = ActivityStack()
    private val actions = NotificationActions()
    private val media = MediaHub { main.post { refreshPill() } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val main = Handler(Looper.getMainLooper())
    private var listener: SkerryListener? = null
    private var allowedPackages: Set<String> = emptySet()
    private var chargingEnabled = false
    private var pluggedIn = false
    private var sessions: List<MediaController> = emptyList()

    private val _islandRunning = MutableStateFlow(false)
    val islandRunning: StateFlow<Boolean> = _islandRunning

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            pluggedIn = readPlugged()
            applyCharging()
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlay = SkerryOverlay(this).also { pill ->
            pill.onCycle = { stack.cycle(); refreshPill() }
            pill.onCenter = { stack.toggleExpanded(); refreshPill() }
            pill.onMedia = { media.toggle() }
            pill.onBody = { openCurrent() }
            pill.onAction = { index -> performCurrent(index) }
            pill.onReply = { text -> replyCurrent(text) }
        }
        val stores = createSkerryStores(this)
        allowlist = stores.allowlist
        charging = stores.charging
        pluggedIn = readPlugged()
        scope.launch {
            allowlist.packages.collect { allowed ->
                allowedPackages = allowed
                reconcile()
                media.update(sessions, allowedPackages)
                refreshPill()
            }
        }
        scope.launch {
            charging.enabled.collect { enabled ->
                chargingEnabled = enabled
                applyCharging()
            }
        }
        registerSystem(
            powerReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            },
        )
        watchOverlayOp()
    }

    fun setIslandRunning(running: Boolean) {
        _islandRunning.value = running
        refreshPill()
    }

    fun attachListener(service: SkerryListener) {
        listener = service
        reconcile()
        refreshPill()
    }

    fun detachListener(service: SkerryListener) {
        if (listener !== service) return
        listener = null
        actions.retain(emptySet())
        stack.replaceNotifications(emptyList())
        sessions = emptyList()
        media.clear()
        refreshPill()
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val item = liveItemFrom(sbn, allowedPackages, packageName) ?: return
        actions.remember(sbn)
        stack.offer(item)
        refreshPill()
    }

    fun onNotificationRemoved(key: String) {
        actions.forget(key)
        stack.remove(key)
        refreshPill()
    }

    fun onSessions(controllers: List<MediaController>) {
        sessions = controllers
        media.update(controllers, allowedPackages)
    }

    fun refreshPill() {
        val current = stack.state().current
        val expanded = stack.state().expanded && current != null
        val mediaSnap = media.snap
        val show = shouldShowPill(
            islandOn = _islandRunning.value,
            overlayAllowed = Settings.canDrawOverlays(this),
            listenerConnected = listener != null,
            hasLiveItem = current != null,
            hasMedia = mediaSnap != null,
        )
        val title = current?.title ?: when (mediaSnap?.playing) {
            true -> getString(R.string.media_playing)
            false -> getString(R.string.media_paused)
            null -> ""
        }
        overlay.render(
            PillModel(
                visible = show,
                expanded = expanded,
                title = title,
                body = if (current != null && expanded) current.text else "",
                actions = if (current != null && expanded) current.actionLabels else emptyList(),
                showReply = expanded && showsReplyField(current),
                showMedia = mediaSnap != null,
                mediaPlaying = mediaSnap?.playing == true,
            ),
        )
        SkerryService.running?.setMediaActive(mediaSnap != null && _islandRunning.value)
    }

    private fun reconcile() {
        val active = listener?.currentNotifications().orEmpty()
        val items = active.mapNotNull { liveItemFrom(it, allowedPackages, packageName) }
            .sortedByDescending { it.postedAt }
        active.filter { sbn -> items.any { it.key == sbn.key } }.forEach(actions::remember)
        stack.replaceNotifications(items)
        actions.retain(stack.state().items.map { it.key }.toSet())
        applyCharging()
    }

    private fun applyCharging() {
        stack.setCharging(chargingItem(chargingEnabled, pluggedIn, getString(R.string.charging_title)))
        refreshPill()
    }

    private fun openCurrent() {
        val key = stack.state().current?.key ?: return
        actions.open(this, key)
    }

    private fun performCurrent(index: Int) {
        val key = stack.state().current?.key ?: return
        actions.perform(this, key, index)
    }

    private fun replyCurrent(text: String) {
        val key = stack.state().current?.key ?: return
        actions.reply(this, key, text)
        stack.collapse()
        refreshPill()
    }

    private fun registerSystem(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun readPlugged(): Boolean {
        val sticky = registerSystem(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        return batteryPlugged(sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
    }

    private fun watchOverlayOp() {
        val appOps = getSystemService(AppOpsManager::class.java)
        appOps.startWatchingMode(
            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
            packageName,
            object : AppOpsManager.OnOpChangedListener {
                override fun onOpChanged(op: String?, packageName: String?) {
                    main.post { refreshPill() }
                }
            },
        )
    }
}
