package com.example.starbox.data

import com.example.starbox.AppPrefs
import com.example.starbox.data.db.MessageDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Deletes inbox messages older than the user's retention setting. Memos are never touched;
 * a memo made from a deleted message keeps its quoted text and just loses the link.
 *
 * Runs opportunistically (app start, incoming notifications) at most every few hours, so no
 * background scheduler is needed.
 */
class RetentionCleaner(
    private val messageDao: MessageDao,
    private val prefs: AppPrefs,
) {
    private val mutex = Mutex()

    suspend fun runIfDue(now: Long = System.currentTimeMillis()) {
        if (now - prefs.lastCleanupAt < MIN_INTERVAL_MS) return
        run(now)
    }

    suspend fun run(now: Long = System.currentTimeMillis()) = mutex.withLock {
        val days = prefs.retentionDays.value
        if (days > 0) messageDao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(days.toLong()))
        prefs.lastCleanupAt = now
    }

    private companion object {
        val MIN_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
    }
}
