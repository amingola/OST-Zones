package com.example.ostzones.api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.R

class PlaylistDemoActivity : AppCompatActivity() {
    private var playlists = mutableListOf<PlaylistResponseData>()

    private lateinit var playlistsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_demo)
        initPlaylistsRecyclerView()
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