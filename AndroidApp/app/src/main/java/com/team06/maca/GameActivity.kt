package com.team06.maca

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.team06.maca.databinding.ActivityGameBinding
import com.team06.maca.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS")!!
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
        adJob = lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                repository.getNextAd().onSuccess {
                    Glide.with(this@GameActivity).load(it).into(binding.adImageView)
                }
                delay(30000) // 30-second rotation
            }
        }
    }

    private fun onCardClicked(position: Int) {
        val card = cards[position]
        if (card.isFaceUp || card.isMatched) {
            return
        }

        binding.gameGridView.isEnabled = false

        if (indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
            card.isFaceUp = true
            binding.gameGridView.isEnabled = true
        } else {
            card.isFaceUp = true
            adapter.notifyItemChanged(position)
            checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        adapter.notifyItemChanged(position)
    }

    private fun checkForMatch(position1: Int, position2: Int) {
        if (cards[position1].imageUrl == cards[position2].imageUrl) {
            cards[position1].isMatched = true
            cards[position2].isMatched = true
            matches++
            binding.matchesTextView.text = "$matches/6 matches"
            binding.gameGridView.isEnabled = true
            if (matches == 6) {
                timer?.cancel()
                lifecycleScope.launch {
                    repository.submitScore("User", elapsedTime)
                    val intent = Intent(this@GameActivity, LeaderboardActivity::class.java)
                    startActivity(intent)
                    finish()
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

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        adJob?.cancel()
    }
}