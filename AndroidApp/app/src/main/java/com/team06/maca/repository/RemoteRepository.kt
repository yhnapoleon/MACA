package com.team06.maca.repository

import com.team06.maca.model.ScoreSubmitRequest
import com.team06.maca.network.RetrofitClient

class RemoteRepository : GameRepository {

    private val apiService = RetrofitClient.instance
    private var authToken: String? = null

    override suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(mapOf("username" to username, "password" to password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                authToken = loginResponse.token
                Result.success(loginResponse.userType)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAds(): Result<List<String>> {
        // This method is unused according to the markdown file.
        // Returning an empty list for now.
        return Result.success(emptyList())
    }

    override suspend fun getImages(count: Int): Result<List<String>> {
        return try {
            val response = apiService.getImages(count)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.images)
            } else {
                Result.failure(Exception("Failed to fetch images: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTop5(): Result<List<Pair<String, Int>>> {
        return try {
            val response = apiService.getTop5()
            if (response.isSuccessful && response.body() != null) {
                val leaderboard = response.body()!!.map { Pair(it.user, it.score) }
                Result.success(leaderboard)
            } else {
                Result.failure(Exception("Failed to fetch leaderboard: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitScore(user: String, score: Int): Result<Unit> {
        if (authToken == null) {
            return Result.failure(Exception("User not authenticated"))
        }

        return try {
            val response = apiService.submitScore("Bearer $authToken", ScoreSubmitRequest(score))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to submit score: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNextAd(): Result<String> {
        return try {
            val response = apiService.getNextAd()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.adUrl)
            } else {
                Result.failure(Exception("Failed to fetch next ad: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
