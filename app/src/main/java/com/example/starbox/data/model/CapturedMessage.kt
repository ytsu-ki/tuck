package com.example.starbox.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A notification that matched at least one rule and was saved to the Priority Inbox. */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["postedAt"]),
        Index(value = ["packageName"]),
    ],
)
data class CapturedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity of the message, used to drop reposts of the same notification. */
    val dedupKey: String,
    val packageName: String,
    val appLabel: String,
    val sender: String?,
    /** Group or conversation name, when the message came from a group chat. */
    val conversation: String?,
    val title: String?,
    val text: String,
    val postedAt: Long,
    val capturedAt: Long = System.currentTimeMillis(),
    val matchedRuleIds: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val isRead: Boolean = false,
    val isHandled: Boolean = false,
    /** StatusBarNotification key, used to open the original notification while it still exists. */
    val notificationKey: String? = null,
)
