package com.example.shelf.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Lists of strings (authors, subjects) are stored as JSON arrays. */
internal class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(serializer, value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = try {
        json.decodeFromString(serializer, value)
    } catch (_: SerializationException) {
        emptyList()
    }

    private companion object {
        val json = Json
        val serializer = ListSerializer(String.serializer())
    }
}
