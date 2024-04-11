package com.example.ostzones.api.auth

import android.util.Log
import com.example.ostzones.BuildConfig
import com.example.ostzones.api.models.ClientCredentials
import com.example.ostzones.api.models.TokenResponse
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Call
import retrofit2.Callback

class AuthInterceptor(private val authToken: String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        val response = chain.proceed(request)
        when (response.code) {
            400 -> handleInvalidClient(response)
            401 -> handleMissingToken(response)
        }
        return response
    }

    private fun handleInvalidClient(response: Response) {
        Log.e("AuthInterceptor", "Invalid client error: ${response.message}")
    }

    //401
    private fun handleMissingToken(response: Response) {
        Log.e("AuthInterceptor", "Unauthorized error: ${response.message}")

        val requestBody = ClientCredentials(
            "client_credentials", BuildConfig.SPOTIFY_CLIENT_ID, BuildConfig.SPOTIFY_CLIENT_SECRET)

        AuthServiceFactory.getAuthService()
            .getToken(requestBody.grantType, requestBody.clientId, requestBody.clientSecret)
            .enqueue(getTokenCallback())
    }

    private fun getTokenCallback() = object :
        Callback<TokenResponse> {
        override fun onResponse(call: Call<TokenResponse>, response: retrofit2.Response<TokenResponse>){
            if (response.isSuccessful) {
                val responseData = response.body()
                val token = responseData?.accessToken
                Log.d("AuthInterceptor", "Token is: $token")
            } else {
                Log.e("AuthInterceptor", "Error: ${response.code()}, ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
            Log.e("AuthInterceptor", "FAILED: Unable to get Spotify API token")
        }
    }

}