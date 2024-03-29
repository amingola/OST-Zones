package com.example.ostzones.api

import com.example.ostzones.api.models.PlaylistsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("/v1/me")
    suspend fun getUserId(): SpotifyUser

    //https://api.spotify.com/v1/users/user123/playlists?limit=10&offset=0
    @GET("/v1/users/{userId}/playlists")
    suspend fun getUserPlaylists(@Path("userId") userId: String): PlaylistsResponse
}