package com.example.ostzones.api

import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val SPOTIFY_BASE_URL = "https://accounts.spotify.com/api/"

object ApiServiceFactory {
    private var apiService: ApiService? = null

    fun getApiService(context: AppCompatActivity): ApiService {
        synchronized(this) {
            if (apiService == null) {
                val client = OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(context))
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(SPOTIFY_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                apiService = retrofit.create(ApiService::class.java)
            }
            return apiService!!
        }
    }
}