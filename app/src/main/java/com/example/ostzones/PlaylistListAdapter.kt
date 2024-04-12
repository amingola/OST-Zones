package com.example.ostzones

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.api.models.Playlist
import com.squareup.picasso.Picasso

class PlaylistListAdapter(private val context: Context,
                          private var playlists: List<Playlist>) :
    RecyclerView.Adapter<PlaylistListAdapter.ViewHolder>() {

    var onItemClick: ((Playlist) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.playlist_list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.playlistName.text = playlist.name

        val numTracks: Long = playlist.tracks?.total ?: 0
        holder.playlistDescription.text =
            context.getString(R.string.playlist_description, numTracks)

        val background =
            if(playlist.isChecked) R.drawable.selected_playlist_background_color
            else R.drawable.unselected_playlist_background_color
        holder.itemView.setBackgroundResource(background)

        val backgroundUrl = playlist.images?.first()?.url
        Picasso.get()
            .load(backgroundUrl)
            .resize(150, 150)
            .into(holder.playlistIcon)

        holder.itemView.setOnClickListener {
            playlist.isChecked = !playlist.isChecked
            notifyItemChanged(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val playlistName: TextView = itemView.findViewById(R.id.playlist_name)
        internal val playlistIcon: ImageView = itemView.findViewById(R.id.playlist_icon)
        internal val playlistDescription: TextView = itemView.findViewById(R.id.playlist_description)
    }
}
