package com.example.ostzones

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.api.models.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SPOTIFY_LOGIN_REQUEST_CODE = 1138

class PlaylistActivity : SpotifyActivity() {

    private var playlists = listOf<Playlist>()

    private lateinit var playlistsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        if(authenticated){
            lifecycleScope.launch {
                loadPlaylists()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_playlists, menu)
        return true
    }

    override suspend fun handleSuccessfulLogin() {
        withContext(Dispatchers.IO) {
            userId = apiService.getUserId().id!!
        }

        loadPlaylists()
    }

    private suspend fun loadPlaylists() {
        withContext(Dispatchers.IO) {
            playlists = apiService.getUserPlaylists(userId!!).items
            runOnUiThread {
                initPlaylistsRecyclerView()
            }
        }
    }

    override fun handlePlaylistActivityFinish(resultCode: Int, data: Intent?) {}

    fun onSaveButtonClick(view: View) {
        val resultIntent = Intent()
        val uris = playlists.filter { p -> p.isChecked }.map { p -> p.uri }.toCollection(ArrayList())

        resultIntent.putStringArrayListExtra("uris", uris)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun initPlaylistsRecyclerView() {
        val listAdapter = PlaylistListAdapter(this, playlists)

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
            uri -> playlists.find { it.uri == uri }?.isChecked = true
        }
    }
}