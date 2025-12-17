package com.team06.maca.network

import com.team06.maca.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: Map<String, String>): Response<LoginResponse>

    @GET("/api/game/images")
    suspend fun getImages(@Query("count") count: Int): Response<ImagesResponse>

    @GET("/api/ads/next")
    suspend fun getNextAd(): Response<AdResponse>

    @POST("/api/scores")
    suspend fun submitScore(
        @Header("Authorization") token: String,
        @Body score: ScoreSubmitRequest
    ): Response<Unit>

    @GET("/api/leaderboard/top5")
    suspend fun getTop5(): Response<List<LeaderboardEntry>>

}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5180/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
