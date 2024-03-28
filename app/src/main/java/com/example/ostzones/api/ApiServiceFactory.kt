package com.example.ostzones.api

import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val SPOTIFY_AUTH_BASE_URL = "https://accounts.spotify.com/api/"
const val SPOTIFY_BASE_URL = "https://api.spotify.com/v1/"

object ApiServiceFactory {
    private var authService: ApiService? = null
    private var apiService: ApiService? = null

    fun getAuthService(): ApiService {
        synchronized(this) {
            if (authService == null) {
                val client = getOkHttpClient(null)
                val retrofit = getRetrofit(client, SPOTIFY_AUTH_BASE_URL)
                authService = retrofit.create(ApiService::class.java)
            }
            return authService!!
        }
    }

    fun getApiService(token: String): ApiService {
        synchronized(this) {
            if (apiService == null) {
                val client = getOkHttpClient(token)
                val retrofit = getRetrofit(client, SPOTIFY_BASE_URL)
                apiService = retrofit.create(ApiService::class.java)
            }
            return apiService!!
        }
    }

    private fun getOkHttpClient(token: String?): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(token))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun getRetrofit(client: OkHttpClient, baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
}