package com.example.ostzones

import android.location.Location
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.spotify.android.appremote.api.PlayerApi
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object Utils {

    //Helper functions to shorten the line length of Toast calls
    fun toast(context: AppCompatActivity, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun longToast(context: AppCompatActivity, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

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

    fun getCentroidMarkerOptions(tappedPoint: LatLng): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            draggable(true)
        }
    }

    fun computeCentroidOfSelectedPolygon(selectedPolygon: Polygon): LatLng {
        return computeCentroidOfPoints(selectedPolygon.points)
    }

    fun computeCentroidOfPoints(points: List<LatLng>): LatLng {
        return points.run {
            val totalLatitude = sumOf { it.latitude }
            val totalLongitude = sumOf { it.longitude }
            LatLng(totalLatitude/size, totalLongitude/size)
        }
    }

    fun calculateNewPosition(centroidStartPosition: LatLng, centroidEndPosition: LatLng, positionToMove: LatLng): LatLng {
        val distance = calculateDistance(centroidStartPosition, centroidEndPosition)
        val bearing = calculateBearing(centroidStartPosition, centroidEndPosition)

        val earthRadius = 6371000.0 //Earth's radius in meters

        val origLat = toRadians(positionToMove.latitude)
        val origLon = toRadians(positionToMove.longitude)
        val angularDistance = distance / earthRadius
        val trueCourse = toRadians(bearing.toDouble())

        val newLat = asin(
            sin(origLat) * cos(angularDistance) +
                    cos(origLat) * sin(angularDistance) * cos(trueCourse)
        )
        val newLon = origLon + atan2(
            sin(trueCourse) * sin(angularDistance) * cos(origLat),
            cos(angularDistance) - sin(origLat) * sin(newLat)
        )

        return LatLng(toDegrees(newLat), toDegrees(newLon))
    }

    //Color values are saved as a signed int, e.g. -65536 = 0xFFFF0000
    fun getColorComponentDecimalValue(colorValue: Int, color: SeekBarColor) =
        String.format("%08x", colorValue).slice(color.intRange).toInt(radix = 16)

    fun playRandom(api: PlayerApi?, ostZone: OstZone): Long? {
        val uris = ostZone.playlistUris
        if(uris.isNullOrEmpty()) return null

        val rand = Random.nextInt(0, uris.size)
        api?.play(ostZone.playlistUris?.get(rand))
        api?.setShuffle(true)

        return ostZone.id
    }

    private fun calculateDistance(startPosition: LatLng, endPosition: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startPosition.latitude, startPosition.longitude,
            endPosition.latitude, endPosition.longitude,
            results
        )
        return results[0]
    }

    private fun calculateBearing(startPosition: LatLng, endPosition: LatLng): Float {
        val start = Location("Start")
        start.latitude = startPosition.latitude
        start.longitude = startPosition.longitude

        val end = Location("End")
        end.latitude = endPosition.latitude
        end.longitude = endPosition.longitude

        return start.bearingTo(end)
    }
}
