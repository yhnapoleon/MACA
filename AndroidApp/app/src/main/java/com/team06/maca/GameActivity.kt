package com.team06.maca

import android.animation.Animator
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.team06.maca.databinding.ActivityGameBinding
import com.team06.maca.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val repository = RepositoryProvider.repository
    private lateinit var cards: MutableList<Card>
    private lateinit var adapter: CardAdapter
    private var indexOfSingleSelectedCard: Int? = null
    private var timer: Timer? = null
    private var elapsedTime = 0
    private var adJob: Job? = null
    private var matches = 0
    private var isGameFinished = false

    // Audio components
    private var bgmPlayer: MediaPlayer? = null
    private lateinit var soundPool: SoundPool
    private var turnoverSoundId: Int = 0
    private var matchSoundId: Int = 0
    private var soundsLoadedCount = 0
    private val soundsToLoad = 2
    private var soundsAreReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeAudio()

        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS")
        if (imageUrls == null || imageUrls.isEmpty()) {
            startActivity(Intent(this@GameActivity, FetchActivity::class.java))
            finish()
            return
        }
        val userType = intent.getStringExtra("USER_TYPE")

        cards = (imageUrls.map { Card(it) } + imageUrls.map { Card(it) }).shuffled().toMutableList()

        adapter = CardAdapter(cards) { position ->
            onCardClicked(position)
        }
        binding.gameGridView.adapter = adapter
        binding.matchesTextView.text = "$matches/6 matches"

        startTimer()

        if (userType == "Free User") {
            binding.adImageView.visibility = View.VISIBLE
            startAdRotation()
        }
    }

    private fun initializeAudio() {
        bgmPlayer = MediaPlayer.create(this, R.raw.bgm)
        bgmPlayer?.isLooping = true

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsLoadedCount++
                if (soundsLoadedCount == soundsToLoad) {
                    soundsAreReady = true
                }
            }
        }
        turnoverSoundId = soundPool.load(this, R.raw.turnover, 1)
        matchSoundId = soundPool.load(this, R.raw.match, 1)
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                elapsedTime++
                runOnUiThread {
                    binding.timerTextView.text = "Time: $elapsedTime"
                }
            }
        }, 1000, 1000)
    }

    private fun startAdRotation() {
        adJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val result = repository.getNextAd()
                    if (result.isSuccess) {
                        val adUrl = result.getOrNull()
                        if (!adUrl.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                Glide.with(this@GameActivity)
                                    .load(adUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(binding.adImageView)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore single failure
                }
                delay(30000)
            }
        }
    }

    private fun onCardClicked(position: Int) {
        val card = cards[position]
        if (card.isFaceUp || card.isMatched || isGameFinished) {
            return
        }

        if (soundsAreReady) {
            soundPool.play(turnoverSoundId, 1f, 1f, 0, 0, 1f)
        }

        binding.gameGridView.isEnabled = false

        if (indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
            card.isFaceUp = true
            adapter.notifyItemChanged(position)
            binding.gameGridView.isEnabled = true
        } else {
            card.isFaceUp = true
            adapter.notifyItemChanged(position)
            checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
    }

    private fun checkForMatch(position1: Int, position2: Int) {
        if (cards[position1].imageUrl == cards[position2].imageUrl) {
            cards[position1].isMatched = true
            cards[position2].isMatched = true
            matches++
            binding.matchesTextView.text = "$matches/6 matches"
            binding.gameGridView.isEnabled = true

            if (soundsAreReady) {
                soundPool.play(matchSoundId, 1f, 1f, 0, 0, 1f)
            }

            if (matches == 6) {
                isGameFinished = true
                timer?.cancel()

                bgmPlayer?.pause()
                showVictoryAnimation()
                
                // Create and play a one-time victory sound
                MediaPlayer.create(this, R.raw.victory).apply {
                    setOnCompletionListener { it.release() } // Clean up after playing
                    start()
                }
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                cards[position1].isFaceUp = false
                cards[position2].isFaceUp = false
                adapter.notifyItemChanged(position1)
                adapter.notifyItemChanged(position2)
                binding.gameGridView.isEnabled = true
            }, 1000)
        }
    }

    private fun showVictoryAnimation() {
        binding.victoryOverlay.visibility = View.VISIBLE
        binding.victoryAnimation.playAnimation()

        binding.victoryAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                binding.continueButton.visibility = View.VISIBLE
                binding.continueButton.animate().alpha(1f).setDuration(500).start()
            }
        })

        binding.continueButton.setOnClickListener {
            lifecycleScope.launch {
                repository.submitScore("User", elapsedTime)
                val intent = Intent(this@GameActivity, LeaderboardActivity::class.java)
                intent.putExtra("ELAPSED_TIME", elapsedTime)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isGameFinished) {
            bgmPlayer?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        bgmPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        adJob?.cancel()

        bgmPlayer?.release()
        bgmPlayer = null
        
        soundPool.release()
    }
}
