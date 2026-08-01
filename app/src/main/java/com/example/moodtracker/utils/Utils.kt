package com.example.moodtracker.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.moodtracker.data.MoodJournal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar


fun getTodayDate(): Long {

    val calendar = Calendar.getInstance()

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )
    calendar.set(
        Calendar.MINUTE,
        0
    )
    calendar.set(
        Calendar.SECOND,
        0
    )
    calendar.set(
        Calendar.MILLISECOND,
        0
    )

    return calendar.timeInMillis
}

@RequiresApi(Build.VERSION_CODES.O)
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun calculateStreak(entries: List<MoodJournal>): Int {

    if (entries.isEmpty()) return 0

    val dates = entries
        .map { it.date }
        .toSet()


    val calendar = Calendar.getInstance()
    calendar.timeInMillis = getTodayDate()

    var streak = 0


    while (true) {

        val currentDate = calendar.timeInMillis

        if (dates.contains(currentDate)) {

            streak++

            calendar.add(
                Calendar.DAY_OF_YEAR,
                -1
            )

        } else {
            break
        }
    }

    return streak
}