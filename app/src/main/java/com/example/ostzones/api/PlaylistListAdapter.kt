package com.example.ostzones.api

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.R
import com.example.ostzones.api.models.Playlist

class PlaylistListAdapter(private val context: Context,
                          var playlists: List<Playlist>) :
    RecyclerView.Adapter<PlaylistListAdapter.ViewHolder>() {

    var onItemClick: ((Playlist) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.ost_zone_list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.bind(playlist)
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.ostZoneNameTextView)
//        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)

        init{
            itemView.setOnClickListener {
                val selectedZone = playlists[adapterPosition]
                onItemClick?.invoke(selectedZone)
            }
        }

        fun bind(playlist: Playlist) {
            textViewName.text = playlist.name
//            textViewDescription.text = ostZone.id.toString()
        }
    }
}
