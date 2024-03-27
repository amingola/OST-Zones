package com.example.ostzones.api

import com.example.ostzones.api.PlaylistRequestData
import com.example.ostzones.api.PlaylistResponseData
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiService {
    @POST("/api/token")
    @FormUrlEncoded
    fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Call<TokenResponseData>

    @POST("/dfjkghdk")
    fun getUserId(@Body requestBody: PlaylistRequestData): Call<PlaylistResponseData>
}