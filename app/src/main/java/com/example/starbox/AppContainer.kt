package com.example.starbox

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.starbox.alerts.AlertNotifier
import com.example.starbox.capture.CaptureProcessor
import com.example.starbox.capture.LiveSourceIntents
import com.example.starbox.capture.ReaderError
import com.example.starbox.capture.RecentNotifications
import com.example.starbox.capture.SourceLoudnessTracker
import com.example.starbox.capture.StarNotificationListener
import com.example.starbox.data.RetentionCleaner
import com.example.starbox.data.db.StarBoxDatabase
import com.example.starbox.data.model.AlertProfile
import com.example.starbox.rules.QuietHours
import com.example.starbox.system.InstalledApps
import com.example.starbox.ui.theme.AccentColor
import com.example.starbox.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Hand-wired app-wide dependencies, created once in [StarBoxApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /**
     * For work that must finish even if the screen that started it goes away. A failure here is
     * logged rather than crashing Tuck, because the notification reader lives in the same process.
     */
    val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error -> Log.w("Tuck", "Background task failed", error) },
    )

    val database: StarBoxDatabase by lazy { StarBoxDatabase.create(appContext) }
    val recentNotifications = RecentNotifications()
    val liveSourceIntents = LiveSourceIntents()
    val sourceLoudness = SourceLoudnessTracker()
    val installedApps = InstalledApps(appContext)
    val prefs = AppPrefs(appContext)
    val captureProcessor by lazy { CaptureProcessor(database.ruleDao(), database.messageDao()) }
    val alertNotifier by lazy { AlertNotifier(appContext, prefs, database.ruleDao()) }
    val retentionCleaner by lazy { RetentionCleaner(database.messageDao(), prefs) }

    /** True while Android has the notification listener bound. */
    val listenerConnected = MutableStateFlow(false)

    /**
     * Asks Android to reconnect the listener and waits until it really is, so the UI can report
     * the outcome. Some phones take over ten seconds to rebind.
     */
    suspend fun reconnectListener(timeoutMillis: Long = 15_000): Boolean {
        if (listenerConnected.value) return true
        StarNotificationListener.requestRebind(appContext)
        return withTimeoutOrNull(timeoutMillis) { listenerConnected.first { it } } != null
    }
}

/** Small settings that don't justify DataStore yet. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("starbox_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    private val _defaultAlert = MutableStateFlow(
        AlertProfile(
            sound = prefs.getBoolean(KEY_ALERT_SOUND, true),
            soundUri = prefs.getString(KEY_ALERT_SOUND_URI, null),
            vibrate = prefs.getBoolean(KEY_ALERT_VIBRATE, true),
            headsUp = prefs.getBoolean(KEY_ALERT_HEADS_UP, true),
        ),
    )

    /** Alert used by every rule that doesn't set its own. */
    val defaultAlert: StateFlow<AlertProfile> = _defaultAlert.asStateFlow()

    fun setDefaultAlert(profile: AlertProfile) {
        prefs.edit {
            putBoolean(KEY_ALERT_SOUND, profile.sound)
            putString(KEY_ALERT_SOUND_URI, profile.soundUri)
            putBoolean(KEY_ALERT_VIBRATE, profile.vibrate)
            putBoolean(KEY_ALERT_HEADS_UP, profile.headsUp)
        }
        _defaultAlert.value = profile
    }

    private val _quietHours = MutableStateFlow(
        QuietHours(
            enabled = prefs.getBoolean(KEY_QUIET_ENABLED, false),
            startMinute = prefs.getInt(KEY_QUIET_START, QuietHours().startMinute),
            endMinute = prefs.getInt(KEY_QUIET_END, QuietHours().endMinute),
        ),
    )
    val quietHours: StateFlow<QuietHours> = _quietHours.asStateFlow()

    fun setQuietHours(value: QuietHours) {
        prefs.edit {
            putBoolean(KEY_QUIET_ENABLED, value.enabled)
            putInt(KEY_QUIET_START, value.startMinute)
            putInt(KEY_QUIET_END, value.endMinute)
        }
        _quietHours.value = value
    }

    private val _retentionDays = MutableStateFlow(prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS))

    /** Inbox messages older than this many days are deleted; 0 keeps them forever. Memos are never deleted. */
    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()

    fun setRetentionDays(days: Int) {
        prefs.edit { putInt(KEY_RETENTION_DAYS, days) }
        _retentionDays.value = days
    }

    private val _accent = MutableStateFlow(AccentColor.fromName(prefs.getString(KEY_ACCENT, null)))

    /** App-wide accent color; rose unless the user picks another in Settings. */
    val accent: StateFlow<AccentColor> = _accent.asStateFlow()

    fun setAccent(value: AccentColor) {
        prefs.edit { putString(KEY_ACCENT, value.name) }
        _accent.value = value
    }

    private val _themeMode = MutableStateFlow(ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null)))

    /** Light, dark, or follow the phone. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(value: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, value.name) }
        _themeMode.value = value
    }

    /** Bumped by "Reset alerts" so every alert channel is recreated under a fresh ID. */
    var alertChannelGeneration: Int
        get() = prefs.getInt(KEY_ALERT_GENERATION, 1)
        set(value) = prefs.edit { putInt(KEY_ALERT_GENERATION, value) }

    var lastCleanupAt: Long
        get() = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CLEANUP, value) }

    private val _readerError = MutableStateFlow(
        prefs.getString(KEY_READER_ERROR, null)?.let { ReaderError(prefs.getLong(KEY_READER_ERROR_AT, 0L), it) },
    )

    /** The last notification the reader skipped because reading it failed. */
    val readerError: StateFlow<ReaderError?> = _readerError.asStateFlow()

    fun recordReaderError(error: Throwable) {
        val recorded = ReaderError.from(error)
        prefs.edit {
            putString(KEY_READER_ERROR, recorded.summary)
            putLong(KEY_READER_ERROR_AT, recorded.at)
        }
        _readerError.value = recorded
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        val RETENTION_OPTIONS = listOf(7, 30, 90, 365, 0)

        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ALERT_SOUND = "alert_sound"
        private const val KEY_ALERT_SOUND_URI = "alert_sound_uri"
        private const val KEY_ALERT_VIBRATE = "alert_vibrate"
        private const val KEY_ALERT_HEADS_UP = "alert_heads_up"
        private const val KEY_QUIET_ENABLED = "quiet_enabled"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_LAST_CLEANUP = "last_cleanup_at"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ALERT_GENERATION = "alert_channel_generation"
        private const val KEY_READER_ERROR = "reader_error"
        private const val KEY_READER_ERROR_AT = "reader_error_at"
    }
}
