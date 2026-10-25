package com.example.starbox.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Memo
import com.example.starbox.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY sortOrder, createdAt")
    suspend fun getEnabled(): List<Rule>

    @Query("SELECT * FROM rules")
    suspend fun getAll(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): Rule?

    @Query("SELECT * FROM rules WHERE id = :id")
    fun observeById(id: Long): Flow<Rule?>

    @Upsert
    suspend fun upsert(rule: Rule): Long

    @Delete
    suspend fun delete(rule: Rule)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<CapturedMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): CapturedMessage?

    /** Returns -1 when a message with the same dedupKey already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: CapturedMessage): Long

    @Update
    suspend fun update(message: CapturedMessage)

    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    /** Handling a message also counts as reading it. */
    @Query("UPDATE messages SET isHandled = :handled, isRead = 1 WHERE id = :id")
    suspend fun setHandled(id: Long, handled: Boolean)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM messages WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Memo>>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: Long): Memo?

    @Upsert
    suspend fun upsert(memo: Memo): Long

    @Query("UPDATE memos SET isDone = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE memos SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Delete
    suspend fun delete(memo: Memo)
}
