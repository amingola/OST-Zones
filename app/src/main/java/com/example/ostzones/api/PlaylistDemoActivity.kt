package com.example.ostzones.api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.BuildConfig
import com.example.ostzones.R
import com.example.ostzones.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaylistDemoActivity : AppCompatActivity() {
    private var token: String? = null
    private var playlists = mutableListOf<PlaylistResponseData>()

    private lateinit var playlistsRecyclerView: RecyclerView

    init {
        getToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_demo)
        initPlaylistsRecyclerView()
    }

    private fun getToken(){
        val requestBody = ClientCredentials(
            "client_credentials",
            BuildConfig.SPOTIFY_CLIENT_ID,
            BuildConfig.SPOTIFY_CLIENT_SECRET
        )
        ApiServiceFactory.getApiService(this)
            .getToken(requestBody.grantType, requestBody.clientId, requestBody.clientSecret)
            .enqueue(object : Callback<TokenResponseData> {
                val context = this@PlaylistDemoActivity

                override fun onResponse(call: Call<TokenResponseData>, response: Response<TokenResponseData>) {
                    if(response.isSuccessful){
                        val responseData = response.body()
                        token = responseData?.accessToken
                        Utils.toast(context, "Token is: $token")
                    }else{
                        Log.e("Spotify API", "Error: ${response.code()}, ${response.errorBody()?.string()}")
                        Utils.toast(context, "Error: Unable to get Spotify API token")
                    }
                }

                override fun onFailure(call: Call<TokenResponseData>, t: Throwable) {
                    Utils.toast(context, "FAILED: Unable to get Spotify API token")
                }
            })
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