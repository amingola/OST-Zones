package com.example.ostzones

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.spotify.android.appremote.api.PlayerApi
import kotlin.math.ln
import kotlin.random.Random

object Utils {

    //Helper functions to shorten the line length of Toast calls
    fun toast(context: AppCompatActivity, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun longToast(context: AppCompatActivity, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun createPolygonOptions(points: List<LatLng>, basicPolygonOptions: HashMap<String, Any>): PolygonOptions {
        return PolygonOptions().apply {
            addAll(points)
            fillColor(basicPolygonOptions[FILL_COLOR_KEY] as Int)
            strokeColor(basicPolygonOptions[STROKE_COLOR_KEY] as Int)
            clickable(basicPolygonOptions[CLICKABLE_KEY] as Boolean)
        }
    }

    fun getFreehandDrawingMarkerOptions(tappedPoint: LatLng, resources: Resources): MarkerOptions {
        val iconBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pen) // Replace R.drawable.your_marker_icon with your actual drawable resource
        val resizedBitmap = Bitmap.createScaledBitmap(iconBitmap, 60, 60, false) // Replace width and height with the desired dimensions
        val icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

        return MarkerOptions().apply{
            position(tappedPoint)
            icon(icon)
            draggable(true)
        }
    }

    fun computeCentroidOfPoints(points: List<LatLng>): LatLng {
        return points.run {
            val totalLatitude = sumOf { it.latitude }
            val totalLongitude = sumOf { it.longitude }
            LatLng(totalLatitude/size, totalLongitude/size)
        }
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

    fun calculateZoomLevel(resources: Resources, polygonPoints: List<LatLng>): Float {
        val builder = LatLngBounds.Builder()
        for (point in polygonPoints) builder.include(point)
        val bounds = builder.build()

        val centroid = computeCentroidOfPoints(polygonPoints)
        val distance = calculateDistance(centroid, bounds.center)
        val screenSize = resources.displayMetrics.widthPixels.toFloat()

        val metersPerPixel = distance / screenSize

        //According to ChatGPT, the significance of the first constant here is
        //"the zoom level at which the map shows the entire world" (16 was provided, which isn't accurate).
        //10 works decently well for zone sizes from that of a house to half the size of the US.
        return ((10 - ln(metersPerPixel.toDouble()) / ln(2.0))).toFloat()
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results)
        return results[0]
    }
}
