package com.team06.maca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(
    private val onImageClick: (Int) -> Unit
) : ListAdapter<String, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = getItem(position)
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

    fun getSelectedImageUrls(): List<String> {
        return selectedPositions.map { getItem(it) }
    }

    private fun clearSelection() {
        val positionsToClear = selectedPositions.toList()
        selectedPositions.clear()
        positionsToClear.forEach { notifyItemChanged(it) }
    }

    override fun submitList(list: List<String>?) {
        clearSelection()
        super.submitList(list)
    }

    override fun submitList(list: List<String>?, commitCallback: Runnable?) {
        clearSelection()
        super.submitList(list, commitCallback)
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
