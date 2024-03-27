package com.example.ostzones.api

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: AppCompatActivity) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) {
            Log.e("AuthInterceptor", "Unauthorized error: ${response.message}")
            Toast.makeText(
                context,
                "This would be the time to refresh the token",  //TODO
                Toast.LENGTH_SHORT
            ).show()
        }
        return response
    }
}