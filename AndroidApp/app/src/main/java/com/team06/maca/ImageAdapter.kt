package com.team06.maca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ImageAdapter(
    private val onImageClick: (Int) -> Unit
) : ListAdapter<DisplayImage, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val displayImage = getItem(position)
        // Disable caching to ensure the latest image is always displayed
        Glide.with(holder.itemView.context)
            .load(displayImage.path)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(holder.imageView)

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

    fun getSelectedImagePaths(): List<String> {
        return selectedPositions.map { getItem(it).path }
    }

    fun clearSelections() {
        val positionsToClear = selectedPositions.toList()
        selectedPositions.clear()
        positionsToClear.forEach { notifyItemChanged(it) }
    }

    override fun submitList(list: List<DisplayImage>?) {
        super.submitList(list)
    }

    override fun submitList(list: List<DisplayImage>?, commitCallback: Runnable?) {
        super.submitList(list, commitCallback)
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<DisplayImage>() {
        override fun areItemsTheSame(oldItem: DisplayImage, newItem: DisplayImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DisplayImage, newItem: DisplayImage): Boolean {
            return oldItem.path == newItem.path
        }
    }
}
