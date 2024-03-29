package com.example.ostzones.api

import com.example.ostzones.api.PlaylistRequestData
import com.example.ostzones.api.PlaylistResponseData
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET

interface ApiService {

    var token: String

    @POST("/api/token")
    @FormUrlEncoded
    fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Call<TokenResponseData>

    @GET("/v1/me")
    fun getUserId(): Call<SpotifyUser>

    //https://api.spotify.com/v1/users/user123/playlists?limit=10&offset=0
    @GET("/users/{userId}/playlists")
    fun getUserPlaylists(@Field("userId") userId: String): Call<PlaylistResponseData>
}