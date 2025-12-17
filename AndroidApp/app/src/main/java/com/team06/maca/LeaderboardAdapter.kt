package com.team06.maca

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private val scores: List<Pair<String, Int>>,
    private val currentScore: Int = -1
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    private var highlighted = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val score = scores[position]
        holder.rankTextView.text = "${position + 1}"
        holder.playerTextView.text = score.first
        holder.scoreTextView.text = score.second.toString()

        if (score.second == currentScore && !highlighted) {
            holder.itemView.setBackgroundColor(Color.YELLOW)
            highlighted = true
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int = scores.size

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView: TextView = itemView.findViewById(R.id.rankTextView)
        val playerTextView: TextView = itemView.findViewById(R.id.playerTextView)
        val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)
    }
}