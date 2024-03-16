package com.example.ostzones

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.Polygon

class OstZoneListAdapter(private val context: Context,
                         private val ostZones: HashMap<Polygon, OstZone>) :
    RecyclerView.Adapter<OstZoneListAdapter.ViewHolder>() {

    var onItemClick: ((OstZone) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.ost_zone_list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ostZone = ostZones.values.toList()[position]
        holder.bind(ostZone)
    }

    override fun getItemCount(): Int {
        return ostZones.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewName) //TODO rename these
//        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)

        init{
            itemView.setOnClickListener {
                val selectedZone = ostZones.values.toList()[adapterPosition]
                onItemClick?.invoke(selectedZone)
            }
        }

        fun bind(ostZone: OstZone) {
            textViewName.text = ostZone.name
//            textViewDescription.text = ostZone.id.toString()
        }
    }
}
