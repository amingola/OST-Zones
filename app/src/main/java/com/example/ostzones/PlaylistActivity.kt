package com.example.ostzones

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.api.ApiService
import com.example.ostzones.api.ApiServiceFactory
import com.example.ostzones.api.models.Playlist
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SPOTIFY_LOGIN_REQUEST_CODE = 1138

class PlaylistActivity : AppCompatActivity() {

    private val logTag = "PlaylistDemoActivity"
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
        setContentView(R.layout.activity_playlist)

        //TODO save auth details and only do auth stuff if it's null

        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        ).setScopes(scopes).build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_playlists, menu)
        return true
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
                        Log.e(logTag, e.message!!)
                    }
                }
            } else if (response.type == AuthorizationResponse.Type.ERROR) {
                Utils.longToast(this, response.error)
            }
        }
    }

    private suspend fun handleSuccessfulLogin(response: AuthorizationResponse) {
        initializeSpotifyAppRemote()

        val token = response.accessToken
        apiService = ApiServiceFactory.getApiService(token)

        withContext(Dispatchers.IO) {
            userId = apiService.getUserId().id!!
        }

        withContext(Dispatchers.IO) {
            playlists = apiService.getUserPlaylists(userId).items
            runOnUiThread {
                initPlaylistsRecyclerView()
            }
        }
    }

    private fun initializeSpotifyAppRemote() {
        val connectionParams = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@PlaylistActivity.spotifyAppRemote = spotifyAppRemote
                    Log.d(logTag, "Connected to Spotify")
                }

                override fun onFailure(throwable: Throwable){
                    when (throwable) {
                        is CouldNotFindSpotifyApp -> {
                            Utils.longToast(this@PlaylistActivity,
                                "You must install spotify to play music from this device!")
                            Log.e(logTag, throwable.message!!)
                        }else ->{
                            Utils.longToast(this@PlaylistActivity,
                                "Couldn't connect to Spotify. Relaunch the app, or open Spotify" +
                                        " and play something to set this as the current device.")
                        }
                    }
                }
            }
        )
    }

    private fun initPlaylistsRecyclerView() {
        val listAdapter = PlaylistListAdapter(this, playlists)
        /*listAdapter.onItemClick = { playlist: Playlist ->
            spotifyAppRemote?.playerApi?.play(playlist.uri)
        }*/

        playlistsRecyclerView = findViewById(R.id.playlists_recycler_view)
        playlistsRecyclerView.also {
            it.adapter = listAdapter
            it.layoutManager = LinearLayoutManager(this)
        }

        initializeExistingPlaylistSelections()
    }

    private fun initializeExistingPlaylistSelections() {
        val selectedPlaylistsUris = intent.getStringArrayListExtra("selectedUris")
        selectedPlaylistsUris?.forEach {
                uri -> playlists.find { playlist -> playlist.uri == uri }?.isChecked = true
        }
    }

    fun onSaveButtonClick(view: View) {
        val str = playlists.filter { p -> p.isChecked }.joinToString(", ") { p -> p.name }
        val uris = playlists.filter { p -> p.isChecked }.map { p -> p.uri }.toCollection(ArrayList())
        Utils.toast(this, str)

        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra("uris", uris)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}