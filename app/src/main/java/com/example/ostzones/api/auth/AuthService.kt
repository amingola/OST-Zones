package com.example.ostzones.api.auth

import com.example.ostzones.api.models.TokenResponse
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface AuthService {
    @POST("/api/token")
    @FormUrlEncoded
    fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Call<TokenResponse>
}