package com.example.moodtracker.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodtracker.data.MoodJournal
import com.example.moodtracker.data.MoodJournalDB
import com.example.moodtracker.data.MoodStatistic
import com.example.moodtracker.utils.calculateStreak
import kotlinx.coroutines.launch

class MoodJournalViewModel(application:Application): AndroidViewModel(application) {

    private val dao = MoodJournalDB.getDatabase(application).moodJournalDao()
    val allMoodJournal = dao.getAllMoodJournal()

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak
    private val _moodStatistics = MutableLiveData<List<MoodStatistic>>()
    val moodStatistics: LiveData<List<MoodStatistic>> =  _moodStatistics

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertMoodJournal(mood: Int, note: String) {
        viewModelScope.launch {
            dao.insert(
                MoodJournal( note = note, mood = mood
                )
            )
            getCurrentStreak()
        }
    }
    suspend fun getMoodByDate(date: Long): MoodJournal? {
        return dao.getMoodByDate(date)
    }

    fun updateMoodJournal(moodJournal: MoodJournal) {
        viewModelScope.launch {
            dao.update(moodJournal)
        }
    }

    fun deleteMoodJournal(moodJournal: MoodJournal) = viewModelScope.launch {
        viewModelScope.launch {
            dao.delete(moodJournal)
            getCurrentStreak()
        }
    }

    fun getCurrentStreak() {
        viewModelScope.launch {
            val entries = dao.getAllMoodJournals()
            val streak = calculateStreak(entries)

            _streak.value = streak
        }
    }

    //For labels at the bottom of barchart
    fun loadMoodStatistics() {

        viewModelScope.launch {
            val journals = dao.getAllMoodJournals()
            val statistics = journals
                .groupingBy { it.mood }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .map {
                    MoodStatistic(
                        mood = it.first,
                        count = it.second
                    )
                }
            _moodStatistics.value = statistics
        }
    }
}