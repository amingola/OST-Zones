package com.example.ostzones

import com.spotify.android.appremote.api.SpotifyAppRemote

interface SpotifyCallback {
    fun setSpotifyAppRemote(spotifyAppRemote: SpotifyAppRemote)
}
