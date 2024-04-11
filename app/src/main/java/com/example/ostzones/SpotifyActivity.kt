package com.example.ostzones

import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.SpotifyAppRemote

abstract class SpotifyActivity : AppCompatActivity() {
    abstract fun setSpotifyAppRemote(spotifyAppRemote: SpotifyAppRemote)
}
