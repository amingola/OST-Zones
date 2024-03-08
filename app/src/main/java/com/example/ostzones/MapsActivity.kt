package com.example.ostzones

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ostzones.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.GoogleMap.OnPolygonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener, OnMarkerDragListener, OnPolygonClickListener {

    private val ostZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markers: ArrayList<Marker> = arrayListOf()
    private var isDrawing = false

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var selectedPolygon: Polygon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapClickListener { tappedPoint -> handleMapTap(tappedPoint) }
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnPolygonClickListener(this)

        //TODO move camera to user's location
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        //First marker is full opacity, only create the OstZone on clicking the first marker
        if(marker.alpha < 1f) return false

        createOstZone()
        Log.d("drawing polygon", "closing the polygon with " + markers.size + " points")

        for(m in markers) m.remove()
        markers.clear()

        return true
    }

    override fun onMarkerDragStart(marker: Marker) {

    }

    override fun onMarkerDrag(marker: Marker) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position));
    }

    override fun onMarkerDragEnd(marker: Marker) {

    }

    override fun onPolygonClick(polygon: Polygon) {
        val str = "just clicked a polygon named: " + ostZones[polygon]?.name
        Log.d("polygon", str)
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
        if(ostZones[polygon]?.name == null) ostZones[polygon]?.name = System.currentTimeMillis().toString()
        selectedPolygon = polygon
    }

    fun something(view: View) {
        selectedPolygon.remove()
        ostZones.remove(selectedPolygon)
    }

    private fun handleMapTap(tappedPoint: LatLng) {
        Log.d("tapped point", "tapped point: " + tappedPoint.latitude + "-" + tappedPoint.longitude)
        googleMap.addMarker(getOstZoneMarker(tappedPoint))?.let { markers.add(it) }
        isDrawing = true
    }

    private fun getOstZoneMarker(tappedPoint: LatLng): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            draggable(true) //TODO draggable
            if(isDrawing) alpha(0.25f)
        }
    }

    private fun createOstZone(){
        val polygon = googleMap.addPolygon(PolygonOptions().apply {
            addAll(markers.map { marker -> marker.position })
            fillColor(Color.RED)
            strokeColor(Color.BLACK)
            clickable(true)
        })
        ostZones[polygon] = OstZone(polygon, null)
        isDrawing = false
    }

}