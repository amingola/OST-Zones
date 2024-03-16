package com.example.ostzones

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions

object Utilities {

    fun createPolygonOptions(points: List<LatLng>, polygonOptions: HashMap<String, Any>): PolygonOptions {
        return PolygonOptions().apply {
            addAll(points)
            fillColor(polygonOptions["fillColor"] as Int)
            strokeColor(polygonOptions["strokeColor"] as Int)
            clickable(polygonOptions["clickable"] as Boolean)
        }
    }

    fun getOstZoneMarkerOptions(tappedPoint: LatLng, bFirstMarker: Boolean): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            if(!bFirstMarker) alpha(0.25f)
            draggable(true)
        }
    }

    fun getCentroidMarkerOptions(tappedPoint: LatLng, bFirstMarker: Boolean): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            draggable(true)
        }
    }

    fun computeCentroidOfSelectedPolygon(selectedPolygon: Polygon): LatLng {
        return computeCentroidOfSelectedPolygon(selectedPolygon.points)
    }

    fun computeCentroidOfSelectedPolygon(points: List<LatLng>): LatLng {
        return points.run {
            val totalLatitude = sumOf { it.latitude }
            val totalLongitude = sumOf { it.longitude }
            LatLng(totalLatitude/size, totalLongitude/size)
        }
    }
}