package com.team06.maca

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private var scores: List<Pair<String, Int>>,
    private val currentScore: Int = -1,
    private val currentUsername: String = ""
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val scoreData = scores[position]
        holder.rankTextView.text = "#${position + 1}"
        holder.playerTextView.text = scoreData.first
        holder.scoreTextView.text = "${scoreData.second}s"

        // Precise Highlight: Check for both username and score match.
        if (scoreData.first == currentUsername && scoreData.second == currentScore) {
            // --- High-Contrast Highlight Style ---
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700")) // Solid Gold

            holder.rankTextView.setTextColor(Color.BLACK)
            holder.playerTextView.setTextColor(Color.BLACK)
            holder.scoreTextView.setTextColor(Color.BLACK)
        } else {
            // --- Default Style ---
            holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_ui_container)

            holder.rankTextView.setTextColor(Color.parseColor("#FFD700")) // Gold text
            holder.playerTextView.setTextColor(Color.WHITE)
            holder.scoreTextView.setTextColor(Color.WHITE)
        }
    }

    override fun getItemCount(): Int = scores.size

    fun updateScores(newScores: List<Pair<String, Int>>) {
        scores = newScores
        notifyDataSetChanged()
    }

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView: TextView = itemView.findViewById(R.id.rankTextView)
        val playerTextView: TextView = itemView.findViewById(R.id.playerTextView)
        val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)
    }
}