package com.example.moodtracker

import android.app.Dialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import android.app.AlertDialog
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodtracker.data.MoodJournal
import com.example.moodtracker.data.MoodStatistic
import com.example.moodtracker.databinding.ActivityMainBinding
import com.example.moodtracker.databinding.LayoutDialogueEditMoodBinding
import com.example.moodtracker.databinding.MoodSelectorBinding
import com.example.moodtracker.model.Mood
import com.example.moodtracker.ui.DayViewContainer
import com.example.moodtracker.ui.MoodJournalAdaptor
import com.example.moodtracker.utils.dpToPx
import com.example.moodtracker.utils.getTodayDate
import com.example.moodtracker.viewmodel.MoodJournalViewModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.MonthDayBinder
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.moodtracker.utils.toLocalDate
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MoodJournalViewModel
    private lateinit var adapter: MoodJournalAdaptor
    private var selectedMood: Mood? = null
    private var selectedMoodCard: MaterialCardView? = null
    private var selectedMoodView: ImageView? = null
    private var keepSplashScreen = true

    private var moodMap = emptyMap<LocalDate, MoodJournal>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            keepSplashScreen
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()

        setupMoodSelector(binding.moodSelector) { mood ->
            selectedMood = mood
        }

        setupButtonSave()
        moodJournalObserve()
        setupCalendar()
        adjustCalendarHeight()
        setupStreak()
        viewModel.getCurrentStreak()

        // code for mood legend at the bottom of barchart
        viewModel.loadMoodStatistics()
        viewModel.moodStatistics.observe(this) { statistics ->

            Log.d("MOOD_STATS", statistics.toString())
            setupMoodBarChart(statistics)

            setupMoodLegend(statistics)

        }
    }

    /**
     * Button Save Function
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonSave() {

        binding.buttonSave.setOnClickListener {

            val text = binding.txtNote.text.toString()
            val mood = selectedMood?.value

            // Check if mood is selected
            if (mood == null) {
                val toast = Toast.makeText(
                    this,
                    "Please select a mood",
                    Toast.LENGTH_SHORT
                )

                toast.setGravity(
                    Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM,
                    0,
                    400
                )
                toast.show()

                return@setOnClickListener
            }

            lifecycleScope.launch {

                val today = getTodayDate()
                val existingMood = viewModel.getMoodByDate(today)
                if (existingMood != null) {
                    showReplaceDialog(existingMood,mood,text
                    )
                } else {
                    viewModel.insertMoodJournal( mood, text )
                    binding.txtNote.text?.clear()
                    resetMoodSelection()
                }
            }
        }
    }


    /*For editting mood entries*/
    private fun showEditDialog(journal: MoodJournal) {

        val dialogBinding =
            LayoutDialogueEditMoodBinding.inflate(layoutInflater)

        dialogBinding.txtEdit.setText(journal.note)

        var selectedMood = Mood.values()
            .first { it.value == journal.mood }

        setupMoodSelector(dialogBinding.moodSelector) { mood ->
            selectedMood = mood

        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->

                val updatedJournal = journal.copy(
                    mood = selectedMood.value,
                    note = dialogBinding.txtEdit.text.toString()
                )

                viewModel.updateMoodJournal(updatedJournal)

            }
            .setNegativeButton("Cancel", null)
            .show()



    }

    private fun showReplaceDialog(
        existing: MoodJournal,
        newMood: Int,
        newNote: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("Replace today's mood?")
            .setMessage("You already have a mood entry today.")
            .setPositiveButton("Replace") { _, _ ->

                val updated = existing.copy(
                    mood = newMood,
                    note = newNote
                )

                viewModel.updateMoodJournal(updated)

                binding.txtNote.text?.clear()

                resetMoodSelection()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                resetMoodSelection()
                dialog.dismiss()

            }
            .show()
    }

    /**
     * Recycler Setup
     */
    private fun setupRecycler(){
        viewModel = ViewModelProvider(this)[MoodJournalViewModel::class.java]

        adapter = MoodJournalAdaptor(   onEdit = {showEditDialog(it)
        },onDelete = {viewModel.deleteMoodJournal(it)})

        binding.recyclerMoodJournal.adapter = adapter
        binding.recyclerMoodJournal.layoutManager = LinearLayoutManager(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moodJournalObserve(){


        viewModel.allMoodJournal.observe(this) { journals ->
            adapter.submitList(journals)
            // Create a map: LocalDate -> MoodJournal
            moodMap = journals.associateBy { journal ->
                journal.date.toLocalDate()
            }
            // Refresh calendar
            binding.calendarView.notifyCalendarChanged()

            adapter.submitList(journals){
                Log.d("MOOD_DEBUG", "Adapter count: ${adapter.itemCount}")
            }

            //update chart
            updateChart(journals)

            // First load finished
            keepSplashScreen = false
        }
    }

    /**
     * Sets up click listeners for all mood icons.
     *
     * When a mood icon is tapped, the selectedMood property is updated
     * with Mood enum value.
     */
    private fun setupMoodSelector(
        moodSelector: MoodSelectorBinding,
        onMoodSelected: (Mood) -> Unit
    ) {

        moodSelector.imgviewExcited.setOnClickListener {

            onMoodSelected(Mood.EXCITED)
            selectMood(
                moodSelector.cardviewExcited,
                moodSelector.imgviewExcited,
                Mood.EXCITED
            )
        }


        moodSelector.imgviewHappy.setOnClickListener {

            onMoodSelected(Mood.HAPPY)
            selectMood(
                moodSelector.cardviewHappy,
                moodSelector.imgviewHappy,
                Mood.HAPPY
            )
        }


        moodSelector.imgviewNeutral.setOnClickListener {

            onMoodSelected(Mood.NEUTRAL)
            selectMood(
                moodSelector.cardviewNeutral,
                moodSelector.imgviewNeutral,
                Mood.NEUTRAL
            )
        }

        moodSelector.imgviewSad.setOnClickListener {

            onMoodSelected(Mood.SAD)
            selectMood(
                moodSelector.cardviewSad,
                moodSelector.imgviewSad,
                Mood.SAD
            )
        }

        moodSelector.imgviewMad.setOnClickListener {

            onMoodSelected(Mood.MAD)
            selectMood(
                moodSelector.cardviewMad,
                moodSelector.imgviewMad,
                Mood.MAD
            )
        }
    }

    /**
     * For mood buttons animation
     * */
    private fun selectMood(
        card: MaterialCardView,
        imageView: ImageView,
        mood: Mood
    ) {

        selectedMood = mood

        // Reset previous card
        selectedMoodCard?.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.transparent)
        )
        // Reset previous icon
        resetMoodSelection()

        // Highlight selected card
        card.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.light_purple)
        )


        // Enlarge selected icon
        imageView.animate().cancel()

        imageView.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(150)
            .start()


        selectedMoodCard = card
        selectedMoodView = imageView
    }

    /**
     * Codes for Calendar
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {

        val currentMonth = YearMonth.now()

        binding.calendarView.dayBinder =
            object : MonthDayBinder<DayViewContainer> {

                override fun create(view: View): DayViewContainer {
                    return DayViewContainer(view)
                }

                override fun bind(   container: DayViewContainer, data: CalendarDay ) {
                    container.dayText.text =
                        data.date.dayOfMonth.toString()

                    val journal = moodMap[data.date]

                    if (journal != null) {

                        when(journal.mood) {

                            1 -> container.moodIcon
                                .setImageResource(R.drawable.ic_mood_mad)

                            2 -> container.moodIcon
                                .setImageResource(R.drawable.ic_mood_sad)

                            3 -> container.moodIcon
                                .setImageResource(R.drawable.ic_mood_neutral)

                            4 -> container.moodIcon
                                .setImageResource(R.drawable.ic_mood_happy)

                            5 -> container.moodIcon
                                .setImageResource(R.drawable.ic_mood_excited)
                        }
                    } else {
                        container.moodIcon.setImageDrawable(null)
                    }

                    container.view.setOnClickListener {
                        journal?.let {
                            showMoodDialog(it)
                        }
                    }
                }
            }


        binding.calendarView.setup(
            currentMonth.minusMonths(12),
            currentMonth.plusMonths(12),
            WeekFields.of(Locale.getDefault()).firstDayOfWeek
        )

        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.monthScrollListener = { month ->
            binding.monthTitle.text =
                "${month.yearMonth.month.name.lowercase()
                    .replaceFirstChar { it.uppercase() }} ${month.yearMonth.year}"
        }
    }


    /*For Calendar Dynamic Height
    *
    * */
    private fun adjustCalendarHeight() {

        binding.calendarView.post {
            val calendarWidth = binding.calendarView.width
            if (calendarWidth > 0) {
                val daySize = calendarWidth / 7

                // 6 rows + extra space for emoji
                val calendarHeight = (daySize * 6) + 20
                binding.calendarView.layoutParams.height = calendarHeight
                binding.calendarView.requestLayout()

                Log.d("CALENDAR_DEBUG","Width: $calendarWidth Height: $calendarHeight"
                )
            }
        }
    }

    /**
     * Show Pop up dialog
     */
    private fun showMoodDialog(journal: MoodJournal) {

        val dialog = Dialog(this)

        val dialogView = layoutInflater.inflate(
            R.layout.layout_dialog_mood,
            null
        )

        dialog.setContentView(dialogView)

        val moodIcon = dialogView.findViewById<ImageView>(R.id.dialogMoodIcon)
        val noteText = dialogView.findViewById<TextView>(R.id.dialogNote)
        val closeButton = dialogView.findViewById<TextView>(R.id.dialogClose)

        // Set mood details
        moodIcon.setImageResource(
            getMoodIcon(journal.mood)
        )

        noteText.text = journal.note
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()
        dialog.window?.setLayout(
            320.dpToPx(this),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


    /**get mood icons**
     *
     */
    private fun getMoodIcon(mood: Int): Int {
        return when (mood) {
            1 -> R.drawable.ic_mood_mad
            2 -> R.drawable.ic_mood_sad
            3 -> R.drawable.ic_mood_neutral
            4 -> R.drawable.ic_mood_happy
            5 -> R.drawable.ic_mood_excited
            else -> R.drawable.ic_mood_neutral
        }
    }

    private fun setupMoodLegend(statistics: List<MoodStatistic>) {

        binding.moodLegend.removeAllViews()
        statistics.forEach { item ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                params.setMargins(
                    20, // left
                    0,
                    20, // right
                    0
                )

                layoutParams = params
            }

            val imageView = ImageView(this)
            imageView.setImageResource(
                getMoodIcon(item.mood)
            )
            imageView.layoutParams = LinearLayout.LayoutParams(
                40,   40
            )

            val textView = TextView(this)
            textView.text = " - ${item.count}"

            binding.moodLegend.addView(itemLayout)
            binding.moodLegend.addView(imageView)
            binding.moodLegend.addView(textView)
        }
    }

    private fun resetMoodSelection() {
        selectedMoodView?.let {
            it.animate().cancel()
            it.scaleX = 1f
            it.scaleY = 1f
        }

        selectedMoodCard?.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.transparent)
        )

        selectedMoodView = null
        selectedMoodCard = null
    }

    private fun setupStreak(){
        viewModel.streak.observe(this) { streak ->

            binding.streakText.text =
                "$streak ${if (streak == 1) "Day" else "Days"} Streak"
        }
    }

    private fun setupMoodBarChart( statistics: List<MoodStatistic>) {

        binding.moodBarChart.setNoDataText("No data yet.")
        if (statistics.isEmpty()) {
            binding.moodBarChart.clear()
            binding.moodBarChart.invalidate()
            return
        }

        binding.moodBarChart.clear()
        val sortedStatistics = statistics.sortedByDescending {
            it.count
        }

        val entries = ArrayList<BarEntry>()
        sortedStatistics.forEachIndexed { index, item ->
            entries.add(
                BarEntry(
                    index.toFloat(),
                    item.count.toFloat()
                )
            )
        }

        val labels = sortedStatistics.map {
            getMoodName(it.mood)
        }

        val colors = sortedStatistics.map {
            getMoodColor(it.mood)
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = false
        dataSet.setColors(colors)

        binding.moodBarChart.data = BarData(dataSet)
        binding.moodBarChart.xAxis.valueFormatter =
            IndexAxisValueFormatter(labels)

        binding.moodBarChart.xAxis.setDrawAxisLine(true)
        // Remove top labels
        binding.moodBarChart.xAxis.setDrawLabels(false)

        // Remove legend
        binding.moodBarChart.legend.isEnabled = false
        // Remove description
        binding.moodBarChart.description.isEnabled = false

        binding.moodBarChart.invalidate()
        binding.moodBarChart.invalidate()

    }

    private fun getMoodName(mood: Int): String {
        return when (mood) {
            1 -> "Mad"
            2 -> "Sad"
            3 -> "Neutral"
            4 -> "Happy"
            5 -> "Excited"
            else -> "Unknown"
        }
    }

    private fun getMoodColor(mood: Int): Int {

        return when (mood) {
            1 -> ContextCompat.getColor(this, R.color.mood_mad)
            2 -> ContextCompat.getColor(this, R.color.mood_sad)
            3 -> ContextCompat.getColor(this, R.color.mood_neutral)
            4 -> ContextCompat.getColor(this, R.color.mood_happy)
            5 -> ContextCompat.getColor(this, R.color.mood_excited)
            else -> ContextCompat.getColor(this, R.color.mood_neutral)
        }
    }

    private fun updateChart(journals: List<MoodJournal>) {
        Log.d(
            "CHART_DEBUG",
            "Updating chart with ${journals.size} entries"
        )

        val statistics = journals
            .groupBy { it.mood }
            .map { (mood, journals) ->
                MoodStatistic(
                    mood = mood,
                    count = journals.size
                )
            }

        setupMoodBarChart(statistics)
        binding.moodBarChart.notifyDataSetChanged()
        binding.moodBarChart.invalidate()
    }


}