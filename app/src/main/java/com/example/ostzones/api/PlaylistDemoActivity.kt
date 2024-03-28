package com.example.ostzones.api

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.BuildConfig
import com.example.ostzones.R
import com.example.ostzones.Utils
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE

const val SPOTIFY_LOGIN_REQUEST_CODE = 1138

class PlaylistDemoActivity : AppCompatActivity() {

    private val redirectUri = "ostzones://callback"
    private val scopes = arrayOf(
        "user-read-private",
        "streaming",
        "playlist-read-private",
        "playlist-read-collaborative"
    )

    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var userId: String

    private var playlists = mutableListOf<PlaylistResponseData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_demo)

        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        ).setScopes(scopes).build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request)
        initPlaylistsRecyclerView()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            if (response.type == AuthorizationResponse.Type.TOKEN) {
                val token = response.accessToken
                ApiServiceFactory.getApiService(token)
                Utils.toast(this, "got token $token")
            } else if (response.type == AuthorizationResponse.Type.ERROR) {
                Utils.toast(this, "got error")
            }
        }
    }

    private fun initPlaylistsRecyclerView() {
        var i = 0
        while(i<20){
            playlists.add(PlaylistResponseData("Playlist " + ++i, i))
        }

        val listAdapter = PlaylistListAdapter(this, playlists)
        listAdapter.onItemClick = { playlist ->
            println("${playlist.key1} clicked")
        }

        playlistsRecyclerView = findViewById(R.id.playlists_recycler_view)
        playlistsRecyclerView.also {
            it.adapter = listAdapter
            it.layoutManager = LinearLayoutManager(this)
        }
    }
}