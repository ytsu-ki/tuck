package com.example.starbox.capture

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.starbox.StarBoxApp
import com.example.starbox.rules.DuplicatePolicy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives a copy of every notification posted on the device.
 *
 * Core principle: Tuck observes and never blocks, hides or changes other apps' notifications,
 * with one exception the user opts into per rule ("Tidy up the original"). Only then, and only
 * after Tuck's own alert was posted, is the matching original dismissed.
 */
class StarNotificationListener : NotificationListenerService() {

    private val container get() = (application as StarBoxApp).container
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error -> skipped(error) },
    )
    private lateinit var parser: NotificationParser

    /**
     * Per notification key, the chat messages its last update showed, so each update of a chat
     * notification only checks what's new. Listener callbacks all run on the main thread.
     */
    private val seenLines = mutableMapOf<String, Set<String>>()

    override fun onCreate() {
        super.onCreate()
        parser = NotificationParser(container.installedApps::label, packageName)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        container.listenerConnected.value = true
        // Seed the diagnostics list with what is already in the shade. These are not run
        // through rules, so connecting never fires alerts for old notifications.
        try {
            val ranking = currentRanking
            seenLines.clear()
            activeNotifications.orEmpty()
                .sortedBy { it.postTime }
                .forEach { sbn ->
                    // One that can't be read is skipped; it must not stop the reader connecting.
                    safely {
                        val post = parser.parse(sbn) ?: return@safely
                        container.recentNotifications.add(post.latest)
                        post.lineIds?.let { seenLines[sbn.key] = it }
                        recordLoudness(sbn, ranking)
                    }
                }
        } catch (_: SecurityException) {
            // Access was revoked between connect and this call.
        }
    }

    override fun onListenerDisconnected() {
        container.listenerConnected.value = false
        requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) = safely {
        val post = parser.parse(sbn, seenLines[sbn.key]) ?: return@safely
        post.lineIds?.let { seenLines[sbn.key] = it }
        post.toCheck.forEach(container.recentNotifications::add)
        recordLoudness(sbn, rankingMap)
        val sourceIntent = sbn.notification.contentIntent
        scope.launch {
            var tidy = false
            // Several new messages can arrive in one update; each one is checked on its own.
            for (message in post.toCheck) {
                val result = container.captureProcessor.process(message) ?: continue
                sourceIntent?.let { container.liveSourceIntents.put(sbn.key, it) }
                val alerted = container.alertNotifier.alert(result, sourceIntent)
                if (DuplicatePolicy.shouldDismissOriginal(result.matchedRules, alerted)) tidy = true
            }
            if (tidy) dismissOriginal(sbn.key)
            // The listener runs for days without the app being opened, so clean up from here too.
            container.retentionCleaner.runIfDue()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) = safely {
        // When Tuck tidied the original away itself, keep its tap action: "Open in WeChat" from
        // Tuck's alert or inbox should still jump to the chat.
        if (reason == REASON_LISTENER_CANCEL) return@safely
        container.liveSourceIntents.remove(sbn.key)
        // Read or dismissed: the chat's next notification starts fresh.
        seenLines.remove(sbn.key)
    }

    /**
     * Any error escaping a listener callback kills Tuck, and after a few crashes in a row Android
     * stops connecting the reader at all (Reconnect can't help, and nothing is captured). So a
     * notification that can't be read is skipped and noted for Settings instead.
     */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            skipped(e)
        }
    }

    private fun skipped(error: Throwable) {
        Log.w(TAG, "Skipped a notification that couldn't be read", error)
        container.prefs.recordReaderError(error)
    }

    /** The one place Tuck touches another app's notification, and only for opted-in rules. */
    private fun dismissOriginal(key: String) {
        try {
            cancelNotification(key)
        } catch (_: SecurityException) {
            // Access was revoked meanwhile; leaving the original is the safe outcome.
        }
    }

    /** Notes whether the source app's notification will ring by itself (see the quiet helper). */
    private fun recordLoudness(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        val ranking = Ranking()
        if (rankingMap?.getRanking(sbn.key, ranking) == true) {
            val channel = ranking.channel
            container.sourceLoudness.record(
                packageName = sbn.packageName,
                importance = ranking.importance,
                channelId = sbn.notification.channelId,
                uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sbn.uid else null,
                sound = channel?.sound != null || channel == null,
                vibrates = channel?.shouldVibrate() ?: true,
            )
        }
    }

    companion object {
        private const val TAG = "TuckReader"

        fun componentName(context: Context) = ComponentName(context, StarNotificationListener::class.java)

        /**
         * Asks Android to bind the listener again. Android never answers, and only acts on it when
         * it was the one that unbound us, so callers watch for [onListenerConnected] themselves.
         */
        fun requestRebind(context: Context) {
            try {
                NotificationListenerService.requestRebind(componentName(context))
            } catch (_: RuntimeException) {
                // Access was turned off meanwhile; the status shows that instead.
            }
        }
    }
}
