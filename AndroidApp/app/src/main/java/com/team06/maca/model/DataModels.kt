package com.team06.maca.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("userType") val userType: String,
    @SerializedName("token") val token: String
)

data class ImagesResponse(
    @SerializedName("images") val images: List<String>
)

data class AdResponse(
    @SerializedName("adUrl") val adUrl: String
)

data class LeaderboardEntry(
    @SerializedName("user") val user: String,
    @SerializedName("score") val score: Int
)

data class ScoreSubmitRequest(
    @SerializedName("score") val score: Int
)
