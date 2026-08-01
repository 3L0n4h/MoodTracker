package com.example.moodtracker.ui

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.moodtracker.R
import com.kizitonwose.calendar.view.ViewContainer

class DayViewContainer(view: View) :

    ViewContainer(view) {

    val dayContainer = view.findViewById<LinearLayout>(R.id.dayContainer)
    val dayText = view.findViewById<TextView>(R.id.dayText)
    val moodIcon = view.findViewById<ImageView>(R.id.moodIcon)
}