package com.example.ostzones

import com.google.android.gms.maps.model.LatLng

class OstZone(val polygonPoints: List<LatLng>,
              val polygonOptions: HashMap<String, Any>,
              var name: String?) {
    val id: Int? = null
}