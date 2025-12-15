package com.team06.maca.repository

import kotlinx.coroutines.delay

class MockRepository : GameRepository {

    private val adImages = listOf(
        "https://via.placeholder.com/300x50.png?text=Ad+1",
        "https://via.placeholder.com/300x50.png?text=Ad+2",
        "https://via.placeholder.com/300x50.png?text=Ad+3"
    )
    private var adIndex = 0

    override suspend fun login(username: String, password: String): Result<String> {
        return when {
            username == "admin" && password == "admin" -> Result.success("Paid User")
            username == "guest" && password == "guest" -> Result.success("Free User")
            else -> Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun getAds(): Result<List<String>> {
        return Result.success(adImages)
    }

    override suspend fun getImages(count: Int): Result<List<String>> {
        return Result.success(emptyList())
    }

    override suspend fun getTop5(): Result<List<Pair<String, Int>>> {
        val leaderboard = listOf(
            "Player1" to 100,
            "Player2" to 90,
            "Player3" to 80,
            "Player4" to 70,
            "Player5" to 60
        )
        return Result.success(leaderboard)
    }

    override suspend fun submitScore(user: String, score: Int): Result<Unit> {
        delay(1000) // Simulate network delay
        println("Score submitted for $user: $score")
        return Result.success(Unit)
    }

    override suspend fun getNextAd(): Result<String> {
        adIndex = (adIndex + 1) % adImages.size
        return Result.success(adImages[adIndex])
    }
}