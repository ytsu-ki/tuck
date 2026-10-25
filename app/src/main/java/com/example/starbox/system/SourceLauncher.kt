package com.example.starbox.system

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.starbox.capture.LiveSourceIntents

/** Opens the app a starred message came from, at the exact chat when we still can. */
object SourceLauncher {

    /** Returns false when neither the original notification's action nor the app could be opened. */
    fun open(context: Context, packageName: String, notificationKey: String?, live: LiveSourceIntents): Boolean {
        val pendingIntent = notificationKey?.let(live::get)
        if (pendingIntent != null) {
            try {
                pendingIntent.send(context, 0, null, null, null, null, launchOptions())
                return true
            } catch (_: PendingIntent.CanceledException) {
                live.remove(notificationKey)
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Android 14+ only lets another app's PendingIntent start an activity if the sender (us, in
     * the foreground after a tap) explicitly lends its permission to do so.
     */
    private fun launchOptions() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        @Suppress("DEPRECATION")
        ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            .toBundle()
    } else {
        null
    }
}
