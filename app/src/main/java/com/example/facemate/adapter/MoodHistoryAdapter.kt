package com.example.facemate.adapter

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.facemate.R
import com.example.facemate.model.MoodHistory
import java.text.SimpleDateFormat
import java.util.*

class MoodHistoryAdapter(
    private val moods: List<MoodHistory>,
    private val onDelete: (MoodHistory, String) -> Unit
) : RecyclerView.Adapter<MoodHistoryAdapter.MoodViewHolder>() {

    inner class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moodText: TextView = itemView.findViewById(R.id.tvMood)
        val noteText: TextView = itemView.findViewById(R.id.tvNote)
        val dateText: TextView = itemView.findViewById(R.id.tvDate)

        init {
            itemView.setOnLongClickListener {
                val mood = moods[adapterPosition]
                onDelete(mood, mood.docId)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_history, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val item = moods[position]
        holder.moodText.text = "Mood: ${item.mood}"
        holder.noteText.text = "Note: ${item.note}"

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        holder.dateText.text = item.timestamp?.let { dateFormat.format(it) } ?: "No Date"
    }

    override fun getItemCount(): Int = moods.size
}
