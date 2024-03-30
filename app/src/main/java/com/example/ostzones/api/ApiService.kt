package com.example.ostzones.api

import com.example.ostzones.api.models.PlaylistsResponse
import com.example.ostzones.api.models.StartResumePlaybackRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("/v1/me")
    suspend fun getUserId(): SpotifyUser

    @GET("/v1/users/{userId}/playlists")
    suspend fun getUserPlaylists(@Path("userId") userId: String): PlaylistsResponse

    @PUT("/v1/me/player/play") //spotify:playlist:23LchstWQG96hKLgiNNpWI
    fun startPlayback(@Body playlist: StartResumePlaybackRequest): Call<PlaylistsResponse>
}