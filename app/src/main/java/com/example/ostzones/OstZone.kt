package com.example.ostzones

import com.google.android.gms.maps.model.LatLng

class OstZone(
    var polygonPoints: List<LatLng>,
    var polygonOptions: HashMap<String, Any>,
    var name: String?) {
    var id: Long? = null
}