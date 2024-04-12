package com.example.ostzones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ostzones.api.ApiService
import com.example.ostzones.api.ApiServiceFactory
import com.example.ostzones.api.auth.SpotifyAppRemoteFactory
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity
import kotlinx.coroutines.launch

internal const val PLAYLIST_ACTIVITY_REQUEST_CODE = 1000

abstract class SpotifyActivity : AppCompatActivity() {

    private val scopes = arrayOf(
        "user-read-private",
        "streaming",
        "user-modify-playback-state",
        "playlist-read-private",
        "playlist-read-collaborative"
    )
    private val logTag = "SpotifyActivity"

    private var token: String? = null
    private var spotifyAppRemote: SpotifyAppRemote? = null

    var authenticated = false
    var userId: String? = null
    lateinit var apiService: ApiService

    abstract suspend fun handleSuccessfulLogin()
    abstract fun handlePlaylistActivityFinish(resultCode: Int, data: Intent?)

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            token = savedInstanceState.getString("token", "")
            userId = savedInstanceState.getString("userId", "")
        }

        if(token == null){
            val request = AuthorizationRequest.Builder(
                BuildConfig.SPOTIFY_CLIENT_ID,
                AuthorizationResponse.Type.TOKEN,
                redirectUri
            ).setScopes(scopes).build()

            AuthorizationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request)
        }else{
            apiService = ApiServiceFactory.getApiService(token!!)
            authenticated = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        token?.let{ outState.putString("token", it) }
        userId.let{ outState.putString("userId", it) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PLAYLIST_ACTIVITY_REQUEST_CODE -> handlePlaylistActivityFinish(resultCode, data)
            LoginActivity.REQUEST_CODE -> handleSpotifyLoginFinish(resultCode, data)
        }
    }

    fun setSpotifyAppRemote(spotifyAppRemote: SpotifyAppRemote){
        this.spotifyAppRemote = spotifyAppRemote
    }

    fun getSpotifyAppRemote(): SpotifyAppRemote? {
        return spotifyAppRemote
    }

    private fun handleSpotifyLoginFinish(resultCode: Int, data: Intent?) {
        val response = AuthorizationClient.getResponse(resultCode, data)

        SpotifyAppRemoteFactory.getSpotifyAppRemote(this)

        token = response.accessToken
        apiService = ApiServiceFactory.getApiService(token!!)

        if (response.type == AuthorizationResponse.Type.TOKEN) {
            lifecycleScope.launch {
                try {
                    handleSuccessfulLogin()
                }
                catch (e: Exception) {
                    Log.e(logTag, e.message!!)
                }
            }
        } else if (response.type == AuthorizationResponse.Type.ERROR) {
            Utils.longToast(this, response.error)
        }
    }

}
