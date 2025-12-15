package com.team06.maca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private var imageUrls: List<String> = emptyList()
    private val selectedPositions = mutableSetOf<Int>()

    fun submitList(list: List<String>) {
        imageUrls = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Glide.with(holder.itemView.context).load(imageUrl).into(holder.imageView)

        // Add visual feedback for selected items
        if (selectedPositions.contains(position)) {
            holder.itemView.alpha = 0.7f
            holder.itemView.scaleX = 0.9f
            holder.itemView.scaleY = 0.9f
        } else {
            holder.itemView.alpha = 1.0f
            holder.itemView.scaleX = 1.0f
            holder.itemView.scaleY = 1.0f
        }

        holder.itemView.setOnClickListener {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
            } else {
                if (selectedPositions.size < 6) {
                    selectedPositions.add(position)
                }
            }
            notifyItemChanged(position)
            onImageClick(selectedPositions.size)
        }
    }

    override fun getItemCount(): Int = imageUrls.size

    fun getSelectedImageUrls(): List<String> {
        return selectedPositions.map { imageUrls[it] }
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}