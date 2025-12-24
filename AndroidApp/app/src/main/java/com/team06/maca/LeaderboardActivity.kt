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
        val currentUsername = intent.getStringExtra("USER_NAME") ?: ""
        val currentUserType = intent.getStringExtra("USER_TYPE") ?: ""

        val hasValidCurrent = elapsedTime != -1 && currentUsername.isNotBlank()

        if (hasValidCurrent) {
            binding.currentScoreTextView.text = "Your Score: $elapsedTime"
        }

        adapter = LeaderboardAdapter(emptyList(), elapsedTime, currentUsername)
        binding.leaderboardRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repository.getTop5().onSuccess {
                val topScores = it.toMutableList() // List<Pair<String, Int>>

                if (hasValidCurrent) {
                    // 移除与本次成绩相同、但用户名为空或占位(Unknown)的项，避免重复
                    topScores.removeAll { score ->
                        score.second == elapsedTime && (score.first.isBlank() || score.first.equals("Unknown", true))
                    }

                    // 如果列表中没有“用户名+成绩”完全一致的记录，则补充一条，保证当前玩家可见
                    if (topScores.none { score -> score.first == currentUsername && score.second == elapsedTime }) {
                        topScores.add(Pair(currentUsername, elapsedTime))
                    }
                }

                // Sort by score and take top 5, then update the adapter.
                val finalScores = topScores.sortedBy { it.second }.take(5)
                adapter.updateScores(finalScores)
            }
        }

        binding.logoutButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("USER_NAME", currentUsername)
            intent.putExtra("USER_TYPE", currentUserType)
            startActivity(intent)
        }
    }
}