package com.example.starbox.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.starbox.AppPrefs
import com.example.starbox.MainActivity
import com.example.starbox.R
import com.example.starbox.capture.CaptureResult
import com.example.starbox.data.db.RuleDao
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.rules.AlertPolicy
import com.example.starbox.system.Permissions
import com.example.starbox.ui.theme.accentRoles
import java.time.LocalTime

/** Posts StarBox's own secondary alert for a starred message. */
class AlertNotifier(
    private val context: Context,
    private val prefs: AppPrefs,
    private val ruleDao: RuleDao,
) {
    private val systemManager = context.getSystemService(NotificationManager::class.java)
    private val compatManager = NotificationManagerCompat.from(context)

    /**
     * Alerts for a newly captured message unless every matched rule is silent (or quiet hours
     * apply). Returns true only if Tuck's own notification was actually posted.
     */
    fun alert(result: CaptureResult, sourceIntent: PendingIntent?): Boolean {
        val now = LocalTime.now()
        val quietNow = prefs.quietHours.value.isActive(now.hour * 60 + now.minute)
        val profile = AlertPolicy.effective(result.matchedRules, prefs.defaultAlert.value, quietNow)
        if (profile.isSilent) return false
        return post(result.message, profile, sourceIntent)
    }

    /** Sends a sample alert so the user can hear and see a profile. */
    fun sendTest(profile: AlertProfile) {
        if (profile.isSilent) return
        val sample = CapturedMessage(
            id = TEST_MESSAGE_ID,
            dedupKey = "test",
            packageName = context.packageName,
            appLabel = context.getString(R.string.app_name),
            sender = context.getString(R.string.alert_test_sender),
            conversation = null,
            title = null,
            text = context.getString(R.string.alert_test_text),
            postedAt = System.currentTimeMillis(),
        )
        post(sample, profile, sourceIntent = null)
    }

    fun cancel(messageId: Long) {
        compatManager.cancel(notificationId(messageId))
    }

    /** Clears every Tuck alert from the shade (all of them are read now). */
    fun cancelAll() {
        compatManager.cancelAll()
    }

    private fun post(message: CapturedMessage, profile: AlertProfile, sourceIntent: PendingIntent?): Boolean {
        if (!canPost()) return false
        val channelId = channelFor(profile)
        val heading = listOfNotNull(message.sender ?: message.title, message.conversation)
            .joinToString(" · ")
            .ifEmpty { message.appLabel }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_star)
            .setContentTitle(heading)
            .setContentText(message.text)
            .setSubText(message.appLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setWhen(message.postedAt)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openInInboxIntent(message.id))
        // Tint the small icon with the user's accent (wallpaper colors are left to the system).
        prefs.accent.value.takeUnless { it.isDynamic }?.let { accent ->
            builder.setColor(accentRoles(accent, dark = false).star)
        }

        if (message.id != TEST_MESSAGE_ID) {
            builder.addAction(0, context.getString(R.string.alert_action_handled), markHandledIntent(message.id))
        }
        if (sourceIntent != null) {
            builder.addAction(0, context.getString(R.string.alert_action_open_in, message.appLabel), sourceIntent)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            compatManager.notify(notificationId(message.id), builder.build())
            return true
        }
        return false
    }

    private fun canPost(): Boolean = compatManager.areNotificationsEnabled()

    /**
     * Creates the channels for the default alert and every rule's own alert, and removes any
     * left over. Run at app start and whenever alert settings change, so StarBox's entry in
     * system settings always shows the real categories (with sound, vibration and banners on)
     * before the first alert ever fires.
     */
    suspend fun ensureChannels() {
        val wanted = buildList {
            add(prefs.defaultAlert.value)
            ruleDao.getAll().filterNot { it.useDefaultAlert }.forEach { add(it.alert) }
        }.filterNot { it.isSilent }
        val ids = wanted.map(::channelFor).toSet()
        // Deleting a channel also removes its alerts from the shade. A message several rules
        // starred uses a merged channel that's in no list above, so keep any still showing.
        val showing = systemManager.activeNotifications.mapNotNull { it.notification.channelId }.toSet()
        systemManager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(AlertPolicy.CHANNEL_PREFIX) && it !in ids && it !in showing }
            .forEach(systemManager::deleteNotificationChannel)
    }

    /**
     * Starts every alert channel over under new IDs with StarBox's settings. Android never lets
     * an app turn sound or banners back on for an existing channel, so this is the only way out
     * after a channel was muted (by the user, or by the phone's defaults).
     */
    suspend fun resetChannels() {
        prefs.alertChannelGeneration = prefs.alertChannelGeneration + 1
        ensureChannels()
    }

    /** What the phone will actually do with an alert for [profile] right now. */
    fun health(profile: AlertProfile): AlertHealth {
        val channel = if (profile.isSilent) null else systemManager.getNotificationChannel(channelFor(profile))
        val audio = context.getSystemService(AudioManager::class.java)
        return AlertHealthCheck.evaluate(
            wanted = profile,
            notificationsEnabled = canPost(),
            channel = channel?.let { ChannelSnapshot(it.importance, it.sound != null, it.shouldVibrate()) },
            doNotDisturb = systemManager.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL,
            ringerMode = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
            notificationVolume = audio?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 1,
        )
    }

    /**
     * The settings pages for fixing [profile]'s alerts, best first: the page with the alert
     * channel's sound and vibration switches (which one depends on the phone, see
     * [Permissions.notificationSettingsIntents]), or Tuck's own page while notifications are off.
     */
    fun settingsIntents(profile: AlertProfile): List<Intent> {
        val channel = if (canPost() && !profile.isSilent) channelFor(profile) else null
        return Permissions.notificationSettingsIntents(context, context.packageName, channel)
    }

    /** Returns the channel for [profile], creating it (with sound, vibration and banner as asked) if needed. */
    private fun channelFor(profile: AlertProfile): String {
        val id = AlertPolicy.channelId(profile, prefs.alertChannelGeneration)
        if (systemManager.getNotificationChannel(id) != null) return id

        systemManager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP, context.getString(R.string.alert_channel_group)),
        )
        val importance = if (profile.headsUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, channelName(profile), importance).apply {
            group = CHANNEL_GROUP
            description = context.getString(R.string.alert_channel_description)
            if (profile.sound) {
                setSound(
                    profile.soundUri?.toUri() ?: Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            } else {
                setSound(null, null)
            }
            enableVibration(profile.vibrate)
            if (profile.vibrate) vibrationPattern = VIBRATION_PATTERN
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        systemManager.createNotificationChannel(channel)
        return id
    }

    private fun channelName(profile: AlertProfile): String {
        val parts = buildList {
            if (profile.sound) add(context.getString(R.string.alert_sound))
            if (profile.vibrate) add(context.getString(R.string.alert_vibrate))
            if (profile.headsUp) add(context.getString(R.string.alert_banner))
        }
        return context.getString(
            R.string.alert_channel_name,
            parts.joinToString(context.getString(R.string.list_separator)),
        )
    }

    private fun openInInboxIntent(messageId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_MESSAGE_ID, messageId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId(messageId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun markHandledIntent(messageId: Long): PendingIntent {
        val intent = Intent(context, AlertActionReceiver::class.java)
            .setAction(AlertActionReceiver.ACTION_MARK_HANDLED)
            .putExtra(AlertActionReceiver.EXTRA_MESSAGE_ID, messageId)
        return PendingIntent.getBroadcast(
            context,
            notificationId(messageId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val TEST_MESSAGE_ID = -1L
        private const val CHANNEL_GROUP = "starbox_alerts"
        private val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

        fun notificationId(messageId: Long): Int = (messageId % Int.MAX_VALUE).toInt()
    }
}
