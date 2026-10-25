package com.example.starbox.data.db

import androidx.room.TypeConverter

/**
 * Stores lists as a single column joined by the ASCII unit separator,
 * a character that cannot be typed into a normal text field.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).map { it.toLong() }

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
