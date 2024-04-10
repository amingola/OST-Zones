package com.example.ostzones

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.spotify.android.appremote.api.PlayerApi
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
}
