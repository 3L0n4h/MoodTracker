package com.example.moodtracker.data


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.moodtracker.utils.getTodayDate

@Entity(tableName = "moodjournal")
data class MoodJournal @RequiresApi(Build.VERSION_CODES.O) constructor(

    @PrimaryKey(autoGenerate=true)
    val id: Int = 0,
    val mood: Int,
    val note: String,
    val date: Long = getTodayDate()

)
