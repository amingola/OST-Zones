package com.example.ostzones

import com.google.android.gms.maps.model.LatLng

class OstZone(
    var name: String?,
    var polygonPoints: List<LatLng>,
    var polygonOptions: HashMap<String, Any>
) {
    var id: Long? = null
    var playlistUris: List<String>? = null

    fun isPointInside(point: LatLng) : Boolean{
        val x = point.longitude
        val y = point.latitude

        var isInside = false
        var i = 0
        var j = polygonPoints.size - 1

        while (i < polygonPoints.size) {
            val xi = polygonPoints[i].longitude
            val yi = polygonPoints[i].latitude
            val xj = polygonPoints[j].longitude
            val yj = polygonPoints[j].latitude

            val bIntersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (bIntersect) isInside = !isInside
            j = i++
        }
        return isInside
    }
}