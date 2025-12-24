package com.team06.maca

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        
        binding.urlEditText.setText("https://www.wallpaperflare.com/search?wallpaper=nature")

        imageAdapter = ImageAdapter { selectedCount ->
            binding.playButton.isEnabled = selectedCount == 6
        }
        binding.imageGridView.adapter = imageAdapter

        binding.fetchButton.setOnClickListener {
            val url = binding.urlEditText.text.toString()
            if (url.isNotBlank()) {
                fetchJob?.cancel()
                isPaused = false
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
            intent.putStringArrayListExtra("IMAGE_PATHS", ArrayList(imageAdapter.getSelectedImagePaths()))
            intent.putExtra("USER_TYPE", getIntent().getStringExtra("USER_TYPE"))
            startActivity(intent)
        }
    }

    private suspend fun fetchAndDownloadImages(url: String) {
        // Initial UI setup on Main Thread
        withContext(Dispatchers.Main) {
            binding.playButton.isEnabled = false
            binding.pauseButton.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.progressText.text = "Starting..."
            imageAdapter.submitList(emptyList())
        }

        try {
            // Move all heavy lifting to the IO dispatcher
            withContext(Dispatchers.IO) {
                val downloadedImagePaths = mutableListOf<String>()

                // HYBRID MODE LOGIC to determine URLs
                val imageUrlsToDownload: List<String> = 
                    if (url.contains("stocksnap", ignoreCase = true) || url.contains("wallpaperflare", ignoreCase = true)) {
                        // --- Mode A: Smart Simulation ---
                        val keyword = when {
                            url.contains("nature", ignoreCase = true) -> "nature"
                            url.contains("car", ignoreCase = true) -> "car"
                            url.contains("food", ignoreCase = true) -> "food"
                            else -> "random"
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FetchActivity, "Simulation Mode for demo. Keyword: '$keyword'", Toast.LENGTH_SHORT).show()
                        }
                        (1..20).map { index -> "https://picsum.photos/seed/$keyword$index/400/600" }
                    } else {
                        // --- Mode B & C: Real Jsoup Crawling ---
                        withContext(Dispatchers.Main) {
                           val mode = if (url.contains("toscrape.com", true) || url.contains("webscraper.io", true)) "Sandbox" else "Generic"
                           Toast.makeText(this@FetchActivity, "Real Crawling Mode ($mode)", Toast.LENGTH_SHORT).show()
                        }
                        
                        val doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .maxBodySize(0)
                            .get()
                        
                        val images = doc.select("img")
                        Log.d("FetchActivity", "Found ${images.size} <img> tags initially.")

                        images.map { img ->
                            var src = img.attr("data-src")
                            if (src.isEmpty()) src = img.attr("abs:src")
                            src
                        }.filter { link ->
                            link.isNotEmpty() &&
                            (link.endsWith(".jpg", ignoreCase = true) || link.endsWith(".jpeg", ignoreCase = true)) &&
                            !link.contains("logo", ignoreCase = true) &&
                            !link.contains("icon", ignoreCase = true) &&
                            !link.contains("user", ignoreCase = true) &&
                            !link.contains("avatar", ignoreCase = true) &&
                            link.length > 25
                        }.distinct().take(20)
                    }

                val totalToDownload = imageUrlsToDownload.size
                withContext(Dispatchers.Main) {
                    binding.progressText.text = "Found $totalToDownload images. Starting download..."
                    if (totalToDownload == 0) {
                        Toast.makeText(this@FetchActivity, "No valid images found at this URL.", Toast.LENGTH_LONG).show()
                    }
                }

                // --- Download Loop (already on IO thread) ---
                for ((index, imageUrl) in imageUrlsToDownload.withIndex()) {
                    ensureActive() // This is now safe

                    while (isPaused) {
                        delay(100)
                    }

                    val file = downloadImage(imageUrl, index)
                    if (file != null) {
                        downloadedImagePaths.add(file.absolutePath)
                    }
                    
                    // Post progress and intermediate results to the Main thread
                    withContext(Dispatchers.Main) {
                        binding.progressBar.progress = ((index + 1) * 100) / totalToDownload
                        binding.progressText.text = "Downloading ${index + 1} of $totalToDownload..."
                        imageAdapter.submitList(downloadedImagePaths.toList()) // Progressive update
                    }
                }
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMessage = when (t) {
                        is java.net.UnknownHostException -> "Cannot resolve host. Check URL and internet."
                        is org.jsoup.HttpStatusException -> "Failed to fetch URL: HTTP ${t.statusCode}. Check if the URL is correct and public."
                        is java.net.SocketTimeoutException -> "Connection timed out. The website may be slow or blocking requests."
                        is IOException -> "A network I/O error occurred: ${t.message}"
                        is javax.net.ssl.SSLHandshakeException -> "SSL Error. The website's certificate may be invalid."
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
        // This function is called from the IO dispatcher, so blocking calls are safe.
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("FetchActivity", "Server responded with ${connection.responseCode} for URL: $imageUrl")
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
            Log.e("FetchActivity", "Failed to download image: $imageUrl", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
    }
}
