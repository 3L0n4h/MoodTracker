package com.example.moodtracker.ui


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtracker.data.MoodJournal
import com.example.moodtracker.databinding.RecyclerItemEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodJournalAdaptor (
    private val onEdit: (MoodJournal) -> Unit,
    private val onDelete: (MoodJournal) -> Unit
) : ListAdapter<MoodJournal, MoodJournalAdaptor.MoodJournalViewHolder>(DiffCallBack()){



    class  MoodJournalViewHolder( val binding: RecyclerItemEntryBinding): RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType:Int ): MoodJournalViewHolder{
        val binding = RecyclerItemEntryBinding.inflate(LayoutInflater.from(parent.context),parent, false)
        return MoodJournalViewHolder(binding)
        Log.d("MOOD_DEBUG", "onCreateViewHolder")
    }

    override fun onBindViewHolder(holder: MoodJournalViewHolder, position: Int) {
        val moodJournal = getItem(position)

        Log.d("MOOD_DEBUG", "onBindViewHolder position=$position")

        with(holder.binding){

            noteText.text = "${moodJournal.note}"
            dateText.text = "${formatDate(moodJournal.date)}"
            imgBtnDelete.setOnClickListener{onDelete(moodJournal)}
            imgBtnEdit.setOnClickListener{onEdit(moodJournal)}
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<MoodJournal>(){
            override fun areItemsTheSame(oldItem: MoodJournal, newItem: MoodJournal) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MoodJournal, newItem: MoodJournal) = oldItem == newItem
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }


}

