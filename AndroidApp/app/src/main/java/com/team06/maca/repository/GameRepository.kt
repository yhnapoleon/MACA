package com.team06.maca.repository

interface GameRepository {
    suspend fun login(username: String, password: String): Result<String>
    suspend fun getAds(): Result<List<String>>
    suspend fun getImages(count: Int): Result<List<String>>
    suspend fun getTop5(): Result<List<Pair<String, Int>>>
    suspend fun submitScore(user: String, score: Int): Result<Unit>
    suspend fun getNextAd(): Result<String>
}
