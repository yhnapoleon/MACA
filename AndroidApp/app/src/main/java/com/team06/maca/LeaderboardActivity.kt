package com.team06.maca

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team06.maca.databinding.ActivityLeaderboardBinding
import com.team06.maca.repository.RepositoryProvider
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val repository = RepositoryProvider.repository
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val elapsedTime = intent.getIntExtra("ELAPSED_TIME", -1)

        if (elapsedTime != -1) {
            binding.currentScoreTextView.text = "Your Score: $elapsedTime"
        }

        adapter = LeaderboardAdapter(emptyList(), elapsedTime)
        binding.leaderboardRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repository.getTop5().onSuccess {
                adapter = LeaderboardAdapter(it, elapsedTime)
                binding.leaderboardRecyclerView.adapter = adapter
            }
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }
}