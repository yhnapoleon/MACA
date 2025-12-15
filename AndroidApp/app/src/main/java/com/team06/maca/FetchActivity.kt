package com.team06.maca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team06.maca.databinding.ActivityFetchBinding
import com.team06.maca.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FetchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFetchBinding
    private lateinit var imageAdapter: ImageAdapter
    private val repository = RepositoryProvider.repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFetchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide URL input and fetch button as we now fetch from the backend
        binding.urlEditText.visibility = View.GONE
        binding.fetchButton.visibility = View.GONE

        imageAdapter = ImageAdapter { selectedCount ->
            binding.playButton.isEnabled = selectedCount == 6
        }
        binding.imageGridView.adapter = imageAdapter

        binding.playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putStringArrayListExtra("IMAGE_URLS", ArrayList(imageAdapter.getSelectedImageUrls()))
            intent.putExtra("USER_TYPE", getIntent().getStringExtra("USER_TYPE"))
            startActivity(intent)
        }

        // Automatically fetch images when the activity is created
        lifecycleScope.launch {
            fetchAndDownloadImages()
        }
    }

    private suspend fun fetchAndDownloadImages() {
        val downloadedImagePaths = mutableListOf<String>()
        withContext(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.progressText.text = "Downloading 0 of 20..."
            imageAdapter.submitList(emptyList())
        }

        try {
            val result = repository.getImages(20)
            result.fold(
                onSuccess = {
                    val imageUrlsToDownload = it.take(20)
                    for ((index, imageUrl) in imageUrlsToDownload.withIndex()) {
                        val file = withContext(Dispatchers.IO) {
                            downloadImage(imageUrl, index)
                        }
                        if (file != null) {
                            downloadedImagePaths.add(file.absolutePath)
                        }
                        withContext(Dispatchers.Main) {
                            binding.progressBar.progress = ((index + 1) * 100) / 20
                            binding.progressText.text = "Downloading ${index + 1} of 20..."
                            imageAdapter.submitList(downloadedImagePaths.toList())
                        }
                    }
                },
                onFailure = { e ->
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FetchActivity, "Error fetching images: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } finally {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
            }
        }
    }

    private fun downloadImage(imageUrl: String, index: Int): File? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val inputStream = connection.inputStream
            val file = File(cacheDir, "image_$index.jpg")
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
