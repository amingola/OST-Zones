package com.example.ostzones.api

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.BuildConfig
import com.example.ostzones.R
import com.example.ostzones.Utils
import com.example.ostzones.api.models.Playlist
import com.example.ostzones.api.models.PlaylistsResponse
import com.example.ostzones.api.models.StartResumePlaybackRequest
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

const val SPOTIFY_LOGIN_REQUEST_CODE = 1138

class PlaylistDemoActivity : AppCompatActivity() {

    private var playlists = listOf<Playlist>()
    private val redirectUri = "ostzones://callback"
    private val scopes = arrayOf(
        "user-read-private",
        "streaming",
        "user-modify-playback-state",
        "playlist-read-private",
        "playlist-read-collaborative"
    )
    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var apiService: ApiService
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_demo)

        //TODO save auth details and only do auth stuff if it's null

        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        ).setScopes(scopes).build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            if (response.type == AuthorizationResponse.Type.TOKEN) {
                lifecycleScope.launch {
                    try {
                        handleSuccessfulLogin(response)
                    } catch (e: Exception) {
                        //TODO Handle exceptions
                    }
                }
            } else if (response.type == AuthorizationResponse.Type.ERROR) {
                Utils.toast(this, "got error")
            }
        }
    }

    private suspend fun handleSuccessfulLogin(response: AuthorizationResponse) {
        val token = response.accessToken
        Utils.toast(this, "got token $token")

        apiService = ApiServiceFactory.getApiService(token)

        withContext(Dispatchers.IO) {
            userId = apiService.getUserId().id!!
        }

        withContext(Dispatchers.IO) {
            playlists = apiService.getUserPlaylists(userId).items
            Log.d("playlists", playlists.toString())
            runOnUiThread {
                initPlaylistsRecyclerView(apiService)
            }
        }
    }

    private fun initPlaylistsRecyclerView(apiService: ApiService) {
        val listAdapter = PlaylistListAdapter(this, playlists)
        listAdapter.onItemClick = { playlist: Playlist ->
            onPlaylistClick(playlist, apiService)
        }

        playlistsRecyclerView = findViewById(R.id.playlists_recycler_view)
        playlistsRecyclerView.also {
            it.adapter = listAdapter
            it.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun onPlaylistClick(playlist: Playlist, apiService: ApiService) {
        println("${playlist.name} clicked")
        apiService.startPlayback(
            StartResumePlaybackRequest(
                "spotify:playlist:23LchstWQG96hKLgiNNpWI", null, null
            )
        ).enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    Utils.toast(this@PlaylistDemoActivity, "Playing Chrono Trigger :)")
                } else {

                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {

            }
        })
    }
}