package com.example.ostzones

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions

object Utilities {

    fun createPolygonFromPoints(points: List<LatLng>, polygonOptions: HashMap<String, Any>): PolygonOptions {
        return PolygonOptions().apply {
            addAll(points)
            fillColor(polygonOptions["fillColor"] as Int)
            strokeColor(polygonOptions["strokeColor"] as Int)
            clickable(polygonOptions["clickable"] as Boolean)
        }
    }

}