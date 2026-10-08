package io.github.mohuddle.skerry.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.github.mohuddle.skerry.MainActivity
import io.github.mohuddle.skerry.R

/**
 * Debug-build helper so device checks can post a local notification or a
 * media session without using another app's private notifications.
 */
class DebugPoster : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_REPLY) {
            consumeReply(intent, context)
            return
        }
        when (intent?.getStringExtra("what")) {
            "media" -> DebugMedia.toggle(context.applicationContext)
            "reply" -> postChat(context, intent, withReply = true)
            else -> postChat(context, intent, withReply = false)
        }
    }

    private fun postChat(context: Context, intent: Intent?, withReply: Boolean) {
        val title = intent?.getStringExtra("title") ?: return
        val text = intent.getStringExtra("text").orEmpty()
        val id = intent.getIntExtra("id", 42)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Debug", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPENED, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (withReply) {
            val remote = RemoteInput.Builder(REPLY_KEY).setLabel("Reply").build()
            val replyIntent = PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, DebugPoster::class.java).setAction(ACTION_REPLY).putExtra("id", id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            builder.addAction(
                NotificationCompat.Action.Builder(0, "Reply", replyIntent)
                    .addRemoteInput(remote)
                    .build(),
            )
        }
        manager.notify(id, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "debug"
        const val REPLY_KEY = "reply"
        private const val ACTION_REPLY = "io.github.mohuddle.skerry.DEBUG_REPLY"

        fun consumeReply(intent: Intent, context: Context) {
            val results = RemoteInput.getResultsFromIntent(intent) ?: return
            val replied = results.getCharSequence(REPLY_KEY) != null
            if (!replied) return
            val id = intent.getIntExtra("id", 42)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Debug", NotificationManager.IMPORTANCE_DEFAULT),
            )
            manager.notify(
                id,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_island)
                    .setContentTitle("Reply received")
                    .setContentText("Sent from the pill")
                    .build(),
            )
        }
    }
}
