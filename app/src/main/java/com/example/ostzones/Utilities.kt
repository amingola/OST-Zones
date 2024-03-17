package com.example.ostzones

import android.location.Location
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

    /*fun calculateNewLatLng(originalLatLng: LatLng, startLatLng: LatLng, endLatLng: LatLng): LatLng {
        val distance = calculateDistance(startLatLng, endLatLng)
        val bearing = calculateBearing(startLatLng, endLatLng)

        val earthRadius = 6371000 // Radius of the Earth in meters

        // Convert distance from meters to radians
        val distanceRadians = distance / earthRadius

        // Convert bearing from degrees to radians
        val bearingRadians = Math.toRadians(bearing)

        // Convert original latitude and longitude to radians
        val originalLatRadians = Math.toRadians(originalLatLng.latitude)
        val originalLngRadians = Math.toRadians(originalLatLng.longitude)

        // Calculate new latitude
        val newLatRadians = asin(sin(originalLatRadians) * cos(distanceRadians) +
                cos(originalLatRadians) * sin(distanceRadians) * cos(bearingRadians))

        // Calculate new longitude
        val newLngRadians = originalLngRadians + atan2(sin(bearingRadians) * sin(distanceRadians) * cos(originalLatRadians),
            cos(distanceRadians) - sin(originalLatRadians) * sin(newLatRadians))

        // Convert new latitude and longitude back to degrees
        val newLat = Math.toDegrees(newLatRadians)
        val newLng = Math.toDegrees(newLngRadians)

        return LatLng(newLat, newLng)
    }

    private fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Double {
        val startLocation = Location("").apply {
            latitude = startLatLng.latitude
            longitude = startLatLng.longitude
        }
        val endLocation = Location("").apply {
            latitude = endLatLng.latitude
            longitude = endLatLng.longitude
        }
        return startLocation.distanceTo(endLocation).toDouble()
    }

    private fun calculateBearing(startLatLng: LatLng, endLatLng: LatLng): Double {
        val startLat = Math.toRadians(startLatLng.latitude)
        val startLng = Math.toRadians(startLatLng.longitude)
        val endLat = Math.toRadians(endLatLng.latitude)
        val endLng = Math.toRadians(endLatLng.longitude)

        val dLng = endLng - startLng

        val y = sin(dLng) * cos(endLat)
        val x = cos(startLat) * sin(endLat) - sin(startLat) * cos(endLat) * cos(dLng)

        val bearing = atan2(y, x)
        return Math.toDegrees(bearing)
    }*/

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