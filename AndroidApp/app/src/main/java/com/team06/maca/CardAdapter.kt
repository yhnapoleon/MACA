package com.team06.maca

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.team06.maca.ui.animation.flipCard

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
            Glide.with(holder.itemView.context)
                .load(card.imagePath)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(holder.imageView)
        } else {
            // Use the new card back drawable
            holder.imageView.setImageResource(R.drawable.back)
        }

        holder.itemView.setOnClickListener {
            if (!card.isMatched) {
                // Perform flip animation first for immediate visual feedback
                if (card.isFaceUp) {
                    // Flip to back
                    flipCard(
                        holder.itemView.context,
                        holder.imageView,
                        R.drawable.back,
                        null,
                        isFaceUp = false
                    )
                } else {
                    // Flip to front (downloaded image). Fetch as Bitmap then flip.
                    Glide.with(holder.itemView.context)
                        .asBitmap()
                        .load(card.imagePath)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .centerCrop()
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                flipCard(
                                    holder.itemView.context,
                                    holder.imageView,
                                    null,
                                    resource,
                                    isFaceUp = true
                                )
                            }
                            override fun onLoadCleared(placeholder: Drawable?) { }
                        })
                }

                // Notify game logic
                onCardClick(position)
            }
        }
    }

    override fun getItemCount(): Int = cards.size

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.cardImageView)
    }
}

data class Card(val imagePath: String, var isFaceUp: Boolean = false, var isMatched: Boolean = false)
