package com.example.moodtracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MoodJournalDao {
    @Query("SELECT * FROM moodjournal")
    fun getAllMoodJournal(): LiveData<List<MoodJournal>>

    @Insert
    suspend fun insert(moodJournal: MoodJournal)

    @Update
    suspend fun update(moodJournal: MoodJournal)

    @Delete
    suspend fun delete(moodJournal: MoodJournal)

    @Query("SELECT * FROM moodjournal WHERE date = :date LIMIT 1")
    suspend fun getMoodByDate(date: Long): MoodJournal?

    //For Streak
    @Query("SELECT * FROM moodjournal ORDER BY date DESC")
    suspend fun getAllMoodJournals(): List<MoodJournal>

    @Update
    suspend fun updateMoodJournal(moodJournal: MoodJournal)



}