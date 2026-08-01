package com.example.moodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MoodJournal::class], version = 1)
abstract class MoodJournalDB:RoomDatabase() {

    abstract fun moodJournalDao(): MoodJournalDao

    companion object{
        @Volatile private   var INSTANCE: MoodJournalDB?=null


        fun getDatabase(context: Context): MoodJournalDB {
            return INSTANCE ?: synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    MoodJournalDB::class.java,
                    "moodjournal_db"
                ).build().also{ INSTANCE = it}

            }
        }
    }
}