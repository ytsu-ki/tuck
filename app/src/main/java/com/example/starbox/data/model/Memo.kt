package com.example.starbox.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A note, idea or to-do. [sourceMessageId] links memos created from an inbox message. */
@Entity(
    tableName = "memos",
    foreignKeys = [
        ForeignKey(
            entity = CapturedMessage::class,
            parentColumns = ["id"],
            childColumns = ["sourceMessageId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["sourceMessageId"]), Index(value = ["updatedAt"])],
)
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val isTodo: Boolean = false,
    val isDone: Boolean = false,
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val sourceMessageId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
