package com.example.starbox.system

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.starbox.capture.StarNotificationListener

/** Snapshot of everything StarBox needs from the system. */
data class PermissionState(
    val notificationAccess: Boolean,
    val postNotifications: Boolean,
    val batteryUnrestricted: Boolean,
)

object Permissions {

    fun current(context: Context) = PermissionState(
        notificationAccess = hasNotificationAccess(context),
        postNotifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
    )

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Opens Tuck's own switch on stock Android 11+. Brand skins rebuild Settings and may route
     * that newer screen somewhere else, so there we open the list, where Tuck is one tap away.
     */
    fun openNotificationAccessSettings(context: Context) {
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && OemBattery.current().stockSettings) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                StarNotificationListener.componentName(context).flattenToString(),
            )
            context.startFirst(detail, list, appDetailsIntent(context.packageName))
        } else {
            context.startFirst(list, appDetailsIntent(context.packageName))
        }
    }

    fun openAppNotificationSettings(context: Context) {
        openNotificationSettingsOf(context, context.packageName)
    }

    /** App info page; on Android 13+ this is where "Allow restricted settings" lives for sideloaded apps. */
    fun openAppDetails(context: Context) {
        context.startFirst(appDetailsIntent(context.packageName))
    }

    @SuppressLint("BatteryLife") // Personal tool distributed outside Play; the direct prompt is the clearest UX.
    fun requestBatteryUnrestricted(context: Context) {
        context.startFirst(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetailsIntent(context.packageName),
        )
    }

    /** An app's notification settings, e.g. to set WeChat to Silent (see [notificationSettingsIntents]). */
    fun openNotificationSettingsOf(context: Context, packageName: String, channelId: String? = null, uid: Int? = null) {
        context.startFirst(*notificationSettingsIntents(context, packageName, channelId, uid).toTypedArray())
    }

    /** Ringer mode and volume sliders. */
    fun openSoundSettings(context: Context) {
        context.startFirst(Intent(Settings.ACTION_SOUND_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    /** Do Not Disturb, where it can be turned off or Tuck allowed through. */
    fun openDoNotDisturbSettings(context: Context) {
        context.startFirst(
            Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS),
            Intent(ACTION_ZEN_MODE_SETTINGS),
            Intent(Settings.ACTION_SOUND_SETTINGS),
        )
    }

    /** Opens the first of [intents] the phone supports; false if none opened. */
    fun openFirst(context: Context, intents: List<Intent>): Boolean = context.startFirst(*intents.toTypedArray())

    /**
     * [packageName]'s notification settings pages, best first, for the page with its sound and
     * vibration switches.
     *
     * Where those switches are depends on the phone. Stock Android (and Samsung) keeps them per
     * category, so the page for [channelId], the category the app's notifications come in, goes
     * first. MIUI/HyperOS, EMUI/MagicOS, ColorOS, OriginOS and Flyme put Ringtone, Vibrate and
     * Banners on the app's own page. Their category page, opened from another app, can be a
     * cut-down screen whose switches don't save, so there the app's page goes first.
     *
     * Every page also gets the app's [uid]: Settings looks an app's settings up by package and
     * uid. Stock Android fills in a missing uid itself; some skins don't, and then show default
     * switches that change nothing. "app_package"/"app_uid" are the names those skins read.
     */
    fun notificationSettingsIntents(
        context: Context,
        packageName: String,
        channelId: String? = null,
        uid: Int? = null,
    ): List<Intent> {
        val appUid = uid ?: uidOf(context, packageName)
        fun Intent.forApp(): Intent = apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(EXTRA_APP_PACKAGE_LEGACY, packageName)
            if (appUid != null) putExtra(EXTRA_APP_UID, appUid)
        }
        val appPage = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).forApp()
        val channelPage = channelId?.let {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).forApp().putExtra(Settings.EXTRA_CHANNEL_ID, it)
        }
        val pages = if (OemBattery.current().stockSettings) {
            listOfNotNull(channelPage, appPage)
        } else {
            listOfNotNull(appPage, channelPage)
        }
        return pages + appDetailsIntent(packageName)
    }

    private fun uidOf(context: Context, packageName: String): Int? =
        if (packageName == context.packageName) {
            context.applicationInfo.uid
        } else {
            try {
                context.packageManager.getApplicationInfo(packageName, 0).uid
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }

    private fun appDetailsIntent(packageName: String) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())

    /**
     * Opens settings on top of Tuck instead of in a task of their own. With NEW_TASK, Android
     * brings back a Settings window that is still open exactly as it was, because it ignores
     * extras when matching. A tap could then show the previous screen, e.g. WeChat's page instead
     * of Tuck's. Some brand skins drop standard screens, so each intent is tried in turn.
     */
    private fun Context.startFirst(vararg intents: Intent): Boolean {
        val activity = findActivity()
        for (intent in intents) {
            try {
                if (activity != null) {
                    activity.startActivity(intent)
                } else {
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return true
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            }
        }
        return false
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    /** Not in the public SDK, but older phones and some brand skins only handle this one. */
    private const val ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"

    /** Hidden Settings.EXTRA_APP_UID, read by stock Settings and the brand skins alike. */
    private const val EXTRA_APP_UID = "app_uid"
    private const val EXTRA_APP_PACKAGE_LEGACY = "app_package"
}
