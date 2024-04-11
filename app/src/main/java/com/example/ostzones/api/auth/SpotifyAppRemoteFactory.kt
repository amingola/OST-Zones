package com.example.ostzones.api.auth

import android.util.Log
import com.example.ostzones.BuildConfig
import com.example.ostzones.R
import com.example.ostzones.SpotifyActivity
import com.example.ostzones.Utils
import com.example.ostzones.redirectUri
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp

object SpotifyAppRemoteFactory {
    private const val logTag = "SpotifyAppRemoteFactory"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    fun getSpotifyAppRemote(context: SpotifyActivity) {
        val connectionParams = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@SpotifyAppRemoteFactory.spotifyAppRemote = spotifyAppRemote
                    Log.d(logTag, "Connected to Spotify")
                    context.setSpotifyAppRemote(spotifyAppRemote)
                }

                override fun onFailure(throwable: Throwable){
                    if (throwable is CouldNotFindSpotifyApp) {
                        val msg = context.getString(R.string.spotify_not_installed_warning)
                        Utils.longToast(context, msg)
                        Log.e(logTag, throwable.message!!)
                    } else {
                        val msg = context.getString(R.string.spotify_failed_to_connect_warning)
                        Utils.longToast(context, msg)
                    }
                }
            }
        )
    }
}