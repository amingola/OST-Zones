package com.example.ostzones.api

import com.example.ostzones.api.models.PlaylistsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("/v1/me")
    suspend fun getUserId(): SpotifyUser

    @GET("/v1/users/{userId}/playlists")
    suspend fun getUserPlaylists(@Path("userId") userId: String): PlaylistsResponse
}