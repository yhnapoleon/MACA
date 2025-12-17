package com.team06.maca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team06.maca.databinding.ActivityFetchBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FetchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFetchBinding
    private lateinit var imageAdapter: ImageAdapter
    private var fetchJob: Job? = null
    @Volatile
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFetchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.urlEditText.visibility = View.VISIBLE
        binding.fetchButton.text = "Fetch Images"

        imageAdapter = ImageAdapter { selectedCount ->
            binding.playButton.isEnabled = selectedCount == 6
        }
        binding.imageGridView.adapter = imageAdapter

        binding.fetchButton.setOnClickListener {
            val url = binding.urlEditText.text.toString()
            if (url.isNotBlank()) {
                fetchJob?.cancel()
                isPaused = false // Reset pause state
                binding.pauseButton.text = "Pause"
                fetchJob = lifecycleScope.launch {
                    fetchAndDownloadImages(url)
                }
            } else {
                Toast.makeText(this@FetchActivity, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.pauseButton.setOnClickListener {
            isPaused = !isPaused
            binding.pauseButton.text = if (isPaused) "Resume" else "Pause"
        }

        binding.playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putStringArrayListExtra("IMAGE_URLS", ArrayList(imageAdapter.getSelectedImageUrls()))
            intent.putExtra("USER_TYPE", getIntent().getStringExtra("USER_TYPE"))
            startActivity(intent)
        }
    }

    private suspend fun fetchAndDownloadImages(url: String) {
        val downloadedImagePaths = mutableListOf<String>()
        // Initial UI setup on the Main thread
        withContext(Dispatchers.Main) {
            binding.playButton.isEnabled = false
            binding.pauseButton.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.progressText.text = "Downloading 0 of 20..."
            imageAdapter.submitList(emptyList())
        }

        try {
            // Switch to a background thread for all heavy lifting (network, parsing, file I/O)
            withContext(Dispatchers.IO) {
                // The definitive fix: Set a max body size to prevent OutOfMemoryErrors
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .maxBodySize(2 * 1024 * 1024) // 2MB limit
                    .get()
                
                val images = doc.select("img[src]")
                val imageUrlsToDownload = images.map { it.attr("abs:src") }.filter { it.isNotEmpty() }.take(20)

                for ((index, imageUrl) in imageUrlsToDownload.withIndex()) {
                    ensureActive() // No longer needs lifecycleScope prefix

                    // Pause loop
                    while (isPaused) {
                        delay(100) // Check every 100ms if we should resume
                    }

                    val file = downloadImage(imageUrl, index)
                    if (file != null) {
                        downloadedImagePaths.add(file.absolutePath)
                    }
                    
                    // Switch back to Main thread only for UI updates
                    withContext(Dispatchers.Main) {
                        binding.progressBar.progress = ((index + 1) * 100) / 20
                        binding.progressText.text = "Downloading ${index + 1} of 20..."
                        imageAdapter.submitList(downloadedImagePaths.toList())
                    }
                }
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMessage = when (t) {
                        is java.net.UnknownHostException -> "Cannot resolve host. Check URL and internet connection."
                        is org.jsoup.HttpStatusException -> "Failed to fetch URL: HTTP ${t.statusCode}"
                        is IOException -> if (t.message?.contains("maxBodySize") == true) {
                            "Webpage is too large to parse (max 2MB)."
                        } else {
                            "Network I/O Error: ${t.message}"
                        }
                        is javax.net.ssl.SSLHandshakeException -> "SSL Error. Try a different URL or network."
                        is java.net.MalformedURLException -> "Invalid URL format entered."
                        is OutOfMemoryError -> "Ran out of memory. Try a page with smaller images."
                        else -> "An unexpected error occurred: ${t.message ?: t.javaClass.simpleName}"
                    }
                    Toast.makeText(this@FetchActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
                binding.pauseButton.visibility = View.GONE
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

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
    }
}
