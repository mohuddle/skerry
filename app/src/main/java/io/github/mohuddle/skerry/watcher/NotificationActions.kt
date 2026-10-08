package io.github.mohuddle.skerry.watcher

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification

/** PendingIntents for the live stack. Dropped when the notification leaves the stack. */
class NotificationActions {
    private val content = mutableMapOf<String, PendingIntent>()
    private val actions = mutableMapOf<String, List<Notification.Action>>()

    fun remember(sbn: StatusBarNotification) {
        sbn.notification.contentIntent?.let { content[sbn.key] = it }
        actions[sbn.key] = sbn.notification.actions?.toList().orEmpty()
    }

    fun forget(key: String) {
        content.remove(key)
        actions.remove(key)
    }

    fun retain(keys: Set<String>) {
        content.keys.retainAll(keys)
        actions.keys.retainAll(keys)
    }

    fun open(context: Context, key: String) {
        send(content[key], context, null)
    }

    fun perform(context: Context, key: String, index: Int) {
        val action = actions[key]?.getOrNull(index) ?: return
        if (!action.remoteInputs.isNullOrEmpty()) return
        send(action.actionIntent, context, null)
    }

    fun reply(context: Context, key: String, text: String) {
        val action = actions[key]?.firstOrNull { !it.remoteInputs.isNullOrEmpty() } ?: return
        val inputs = action.remoteInputs ?: return
        val fillIn = Intent()
        val results = Bundle()
        inputs.forEach { input -> results.putCharSequence(input.resultKey, text) }
        RemoteInput.addResultsToIntent(inputs, fillIn, results)
        send(action.actionIntent, context, fillIn)
    }

    private fun send(intent: PendingIntent?, context: Context, fillIn: Intent?) {
        if (intent == null) return
        try {
            intent.send(context, 0, fillIn)
        } catch (_: PendingIntent.CanceledException) {
            // The target app dropped the intent.
        }
    }
}
