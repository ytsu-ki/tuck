package com.example.starbox.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined "starred message" rule.
 *
 * Fields are combined with AND; values inside one list are combined with OR.
 * An empty list matches anything.
 */
@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    /** Package names of source apps, e.g. "com.tencent.mm". */
    val packages: List<String> = emptyList(),
    /** Sender, contact, group or account names (case-insensitive contains). */
    val senders: List<String> = emptyList(),
    /** Keywords searched in the message text (case-insensitive contains). */
    val keywords: List<String> = emptyList(),
    /** When true the global default alert is used and [alert] is ignored. */
    val useDefaultAlert: Boolean = true,
    @Embedded(prefix = "alert_") val alert: AlertProfile = AlertProfile(),
    /** Still alerts during quiet hours, for messages that can't wait. Added in schema v2. */
    @ColumnInfo(defaultValue = "0") val bypassQuietHours: Boolean = false,
    /**
     * Opt-in: once Tuck has posted its own alert for a match, dismiss the source app's
     * notification so the same message isn't shown twice. Added in schema v3.
     */
    @ColumnInfo(defaultValue = "0") val dismissOriginal: Boolean = false,
    val tag: String? = null,
    /** ARGB color for the tag chip. */
    val color: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** How StarBox re-alerts for a match. All flags off means silent collection. */
data class AlertProfile(
    val sound: Boolean = true,
    /** Custom ringtone URI; null means the system default notification sound. */
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val headsUp: Boolean = true,
) {
    val isSilent: Boolean get() = !sound && !vibrate && !headsUp
}
