package com.example.ostzones.api

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ostzones.BuildConfig
import com.example.ostzones.Utils
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Call
import retrofit2.Callback

class AuthInterceptor(private val context: AppCompatActivity) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) {
            handleMissingToken(response)
        }
        return response
    }

    //401
    private fun handleMissingToken(response: Response) {
        Log.e("AuthInterceptor", "Unauthorized error: ${response.message}")
        Utils.toast(context, "This would be the time to refresh the token")

        getToken()
    }

    private fun getToken(){
        val requestBody = ClientCredentials(
            "client_credentials", BuildConfig.SPOTIFY_CLIENT_ID, BuildConfig.SPOTIFY_CLIENT_SECRET)

        ApiServiceFactory.getAuthService(context)
            .getToken(requestBody.grantType, requestBody.clientId, requestBody.clientSecret)
            .enqueue(getTokenCallback(context))
    }

    private fun getTokenCallback(context: AppCompatActivity) = object :
        Callback<TokenResponseData> {
        override fun onResponse(call: Call<TokenResponseData>, response: retrofit2.Response<TokenResponseData>){
            if (response.isSuccessful) {
                val responseData = response.body()
                val token = responseData?.accessToken
                Utils.toast(context, "Token is: $token")
            } else {
                Log.e("Spotify API", "Error: ${response.code()}, ${response.errorBody()?.string()}")
                Utils.toast(context, "Error: Unable to get Spotify API token")
            }
        }

        override fun onFailure(call: Call<TokenResponseData>, t: Throwable) {
            Utils.toast(context, "FAILED: Unable to get Spotify API token")
        }
    }

}