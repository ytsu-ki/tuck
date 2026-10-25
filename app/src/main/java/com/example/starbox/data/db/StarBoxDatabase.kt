package com.example.starbox.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.starbox.data.model.CapturedMessage
import com.example.starbox.data.model.Memo
import com.example.starbox.data.model.Rule

@Database(
    entities = [Rule::class, CapturedMessage::class, Memo::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: rules.bypassQuietHours
        AutoMigration(from = 1, to = 2),
        // v3: rules.dismissOriginal
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class StarBoxDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun messageDao(): MessageDao
    abstract fun memoDao(): MemoDao

    companion object {
        fun create(context: Context): StarBoxDatabase =
            Room.databaseBuilder(context, StarBoxDatabase::class.java, "starbox.db").build()
    }
}
