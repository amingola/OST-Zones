package com.example.ostzones

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.api.models.Playlist

class PlaylistListAdapter(private val context: Context,
                          var playlists: List<Playlist>) :
    RecyclerView.Adapter<PlaylistListAdapter.ViewHolder>() {

    var onItemClick: ((Playlist) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.playlist_list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.checkBox.text = playlist.name
        holder.checkBox.isChecked = playlist.isChecked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            playlist.isChecked = isChecked
        }

        holder.bind(playlist)
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val textViewName: TextView = itemView.findViewById(R.id.ostZoneNameTextView)
//        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val checkBox: CheckBox = itemView.findViewById(R.id.playlist_checkbox)

        init{
            /*itemView.setOnClickListener {
                val selectedTrack = playlists[adapterPosition]
                onItemClick?.invoke(selectedTrack)
            }*/
        }

        fun bind(playlist: Playlist) {
//            textViewName.text = playlist.name
//            textViewDescription.text = ostZone.id.toString()
        }
    }
}
