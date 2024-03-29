package com.example.ostzones.api

const val SPOTIFY_BASE_URL = "https://api.spotify.com"

object ApiServiceFactory {
    private var apiService: ApiService? = null

    fun getApiService(token: String): ApiService {
        synchronized(this) {
            if (apiService == null) {
                val client = ApiUtils.getOkHttpClient(token)
                val retrofit = ApiUtils.getRetrofit(client, SPOTIFY_BASE_URL)
                apiService = retrofit.create(ApiService::class.java)
            }
            return apiService!!
        }
    }
}