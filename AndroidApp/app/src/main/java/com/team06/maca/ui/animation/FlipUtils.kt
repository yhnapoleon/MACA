package com.team06.maca.ui.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView

/**
 * Flips an ImageView with a realistic 3D effect and swaps its image source midway.
 *
 * Either pass [newImageResId] for a drawable resource (e.g., card back),
 * or [newImageBitmap] for a downloaded front image. One of them must be non-null.
 */
fun flipCard(
    context: Context,
    view: View,
    newImageResId: Int?,
    newImageBitmap: Bitmap?,
    isFaceUp: Boolean
) {
    val imageView = view as? ImageView ?: return

    // Increase camera distance to enhance the 3D depth effect
    val scale = view.resources.displayMetrics.density
    view.cameraDistance = 8000f * scale

    val totalDuration = 350L
    val half = totalDuration / 2

    val firstHalf = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f).apply {
        duration = half
    }
    firstHalf.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            // Swap image exactly at 90 degrees (when the view is "edge-on")
            if (newImageBitmap != null) {
                imageView.setImageBitmap(newImageBitmap)
            } else if (newImageResId != null) {
                imageView.setImageResource(newImageResId)
            }

            // Immediately set to -90 so the second half continues naturally
            view.rotationY = -90f

            // Second half of the flip back to 0 degrees
            ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f).apply {
                duration = half
            }.start()
        }
    })

    firstHalf.start()
}


