package io.github.mohuddle.skerry.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Swiping the "Skerry is on" notice delivers this and stops the island. */
class IslandDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.stopService(Intent(context, SkerryService::class.java))
    }
}
