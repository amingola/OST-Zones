package com.example.ostzones.api

import com.google.gson.annotations.SerializedName

data class TokenResponseData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)