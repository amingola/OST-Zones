package com.example.ostzones.api

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ostzones.BuildConfig
import com.example.ostzones.Utils
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
        if (response.code == 401) {
            handleMissingToken(response)
        }
        return response
    }

    //401
    private fun handleMissingToken(response: Response) {
        Log.e("AuthInterceptor", "Unauthorized error: ${response.message}")
        getToken()
    }

    private fun getToken(){
        val requestBody = ClientCredentials(
            "client_credentials", BuildConfig.SPOTIFY_CLIENT_ID, BuildConfig.SPOTIFY_CLIENT_SECRET)

        ApiServiceFactory.getAuthService()
            .getToken(requestBody.grantType, requestBody.clientId, requestBody.clientSecret)
            .enqueue(getTokenCallback())
    }

    private fun getTokenCallback() = object :
        Callback<TokenResponseData> {
        override fun onResponse(call: Call<TokenResponseData>, response: retrofit2.Response<TokenResponseData>){
            if (response.isSuccessful) {
                val responseData = response.body()
                val token = responseData?.accessToken
                Log.d("AuthInterceptor", "Token is: $token")
            } else {
                Log.e("Spotify API", "Error: ${response.code()}, ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<TokenResponseData>, t: Throwable) {
            Log.e("AuthInterceptor", "FAILED: Unable to get Spotify API token")
        }
    }

}