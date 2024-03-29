package com.example.ostzones.api.auth

import com.example.ostzones.api.ApiUtils

const val SPOTIFY_AUTH_BASE_URL = "https://accounts.spotify.com"

object AuthServiceFactory {
    private var authService: AuthService? = null

    fun getAuthService(): AuthService {
        synchronized(this) {
            if (authService == null) {
                val client = ApiUtils.getOkHttpClient(null)
                val retrofit = ApiUtils.getRetrofit(client, SPOTIFY_AUTH_BASE_URL)
                authService = retrofit.create(AuthService::class.java)
            }
            return authService!!
        }
    }
}