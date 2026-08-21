package com.example.callmind.data.local

import androidx.room.TypeConverter
import com.example.callmind.data.local.entities.CallType

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromCallType(value: CallType): String {
        return value.name
    }

    @TypeConverter
    fun toCallType(value: String): CallType {
        return CallType.valueOf(value)
    }

    @TypeConverter
    fun fromReminderType(value: com.example.callmind.data.local.entities.ReminderType): String {
        return value.name
    }

    @TypeConverter
    fun toReminderType(value: String): com.example.callmind.data.local.entities.ReminderType {
        return com.example.callmind.data.local.entities.ReminderType.valueOf(value)
    }
}
