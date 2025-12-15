package com.team06.maca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CardAdapter(
    private val cards: List<Card>,
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        // Set alpha for matched cards
        if (card.isMatched) {
            holder.itemView.alpha = 0.5f
        } else {
            holder.itemView.alpha = 1.0f
        }

        if (card.isFaceUp) {
            Glide.with(holder.itemView.context).load(card.imageUrl).into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_card_back)
        }

        holder.itemView.setOnClickListener {
            if (!card.isMatched) {
                onCardClick(position)
            }
        }
    }

    override fun getItemCount(): Int = cards.size

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.cardImageView)
    }
}

data class Card(val imageUrl: String, var isFaceUp: Boolean = false, var isMatched: Boolean = false)