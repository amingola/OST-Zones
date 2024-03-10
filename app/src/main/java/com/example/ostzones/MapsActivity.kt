package com.example.ostzones

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.android.material.bottomsheet.BottomSheetBehavior


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener, OnMarkerDragListener, OnPolygonClickListener {

    private val ostZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markers: ArrayList<Marker> = arrayListOf()
    private var isDrawing = false
    private var selectedPolygon: Polygon? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
        //Only create the OstZone on clicking the first marker, and if there's >2 markers
        //Only the first marker is full opacity
        if(marker.alpha < 1f || markers.size < 3) return false

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
        selectedPolygon = polygon
        val ostZone = ostZones[polygon]
        Log.d("polygon", "just clicked a polygon named: " + ostZone?.name)

        (findViewById<TextView>(R.id.zoneName)).text = ostZone!!.name
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        showBottomSheetEditFunctionality()
    }

    fun deleteSelectedZone(view: View) {
        selectedPolygon?.remove()
        ostZones.remove(selectedPolygon)
        selectedPolygon = null
        (findViewById<TextView>(R.id.zoneName)).text = ""
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        showBottomSheetPlaceholder()
    }

    fun editZone(view: View) {
        Toast.makeText(this, "Editing " + selectedZone()?.name, Toast.LENGTH_SHORT).show()
    }

    fun zonesNavClick(item: MenuItem) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    fun ostsNavClick(item: MenuItem) {
        Toast.makeText(this, "OSTs", Toast.LENGTH_SHORT).show()
        //bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    private fun handleMapTap(tappedPoint: LatLng) {
        if(selectedPolygon != null){
            deselectZone()
            return
        }
        Log.d("tapped point", "tapped point: " + tappedPoint.latitude + "-" + tappedPoint.longitude)
        googleMap.addMarker(getOstZoneMarker(tappedPoint))?.let { markers.add(it) }
        isDrawing = true
    }

    private fun deselectZone(){
        selectedPolygon = null
        (findViewById<TextView>(R.id.zoneName)).text = ""
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun getOstZoneMarker(tappedPoint: LatLng): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            draggable(true)
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
        ostZones[polygon] = OstZone(polygon, "The Circle" + (ostZones.size + 1))
        isDrawing = false
    }

    private fun showBottomSheetEditFunctionality() {
        findViewById<View>(R.id.bottom_sheet_functionality).visibility = View.VISIBLE
        findViewById<View>(R.id.bottom_sheet_placeholder).visibility = View.GONE
    }

    private fun showBottomSheetPlaceholder() {
        findViewById<View>(R.id.bottom_sheet_functionality).visibility = View.GONE
        findViewById<View>(R.id.bottom_sheet_placeholder).visibility = View.VISIBLE
    }

    private fun selectedZone() = ostZones[selectedPolygon]

}