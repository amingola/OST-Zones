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
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.AuthenticationFailedException
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.SpotifyDisconnectedException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.client.error.RemoteClientException

const val SPOTIFY_LOGIN_REQUEST_CODE = 1138

class PlaylistDemoActivity : AppCompatActivity() {

    private val redirectUri = "com.example.ostzones://login"
    private val scopes = arrayOf(
        "user-read-private",
        "streaming",
        "user-modify-playback-state",
        "playlist-read-private",
        "playlist-read-collaborative"
    )

    private var playlists = listOf<Playlist>()
    private var spotifyAppRemote: SpotifyAppRemote? = null

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
                Utils.longToast(this, response.error)
            }
        }
    }

    private suspend fun handleSuccessfulLogin(response: AuthorizationResponse) {

        initializeSpotifyAppRemote(response.accessToken)

        val token = response.accessToken
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
        spotifyAppRemote?.playerApi?.play(playlist.uri)
        /*apiService.startPlayback(
            StartResumePlaybackRequest(playlist.uri, null, null)
        ).enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    Utils.toast(this@PlaylistDemoActivity, "Playing ${playlist.name}")
                } else {

                }
            }
            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {

            }
        })*/
    }

    private fun initializeSpotifyAppRemote(accessToken: String) {
        val connectionParams = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(redirectUri)
//            .setAccessToken(accessToken)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@PlaylistDemoActivity.spotifyAppRemote = spotifyAppRemote
                    Log.d("PlaylistDemoActivity", "Connected to Spotify")
                    Utils.longToast(this@PlaylistDemoActivity, "Connected to Spotify")
                }

                override fun onFailure(throwable: Throwable){
                    when (throwable) {
                        is RemoteClientException -> {
                            Log.e("PlaylistDemoActivity", throwable.message!!)
                        }
                        is UserNotAuthorizedException -> {
                            Log.e("PlaylistDemoActivity", throwable.message!!)
                        }
                        is CouldNotFindSpotifyApp -> {
                            Log.e("PlaylistDemoActivity", throwable.message!!)
                        }
                        is AuthenticationFailedException -> {
                            Log.e("PlaylistDemoActivity", throwable.message!!)
                        }
                        is SpotifyDisconnectedException -> {
                            Log.e("PlaylistDemoActivity", throwable.message!!)
                        }
                    }
                }
            }
        )
    }
}