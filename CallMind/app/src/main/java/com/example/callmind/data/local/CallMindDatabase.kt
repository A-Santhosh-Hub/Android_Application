package com.example.callmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.callmind.data.local.daos.NoteDao
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.ContactEntity
import com.example.callmind.data.local.entities.NoteEntity
import com.example.callmind.data.local.entities.ReminderEntity

@Database(
    entities = [
        NoteEntity::class,
        ReminderEntity::class,
        ContactEntity::class,
        CallRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CallMindDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): com.example.callmind.data.local.daos.ReminderDao
}
