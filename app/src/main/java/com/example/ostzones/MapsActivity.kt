package com.example.ostzones

import DatabaseHelper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener, OnMarkerDragListener, OnPolygonClickListener {

    private val ostZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markers: ArrayList<Marker> = arrayListOf()
    private val databaseHelper = DatabaseHelper(this)
    private val polygonOptions = hashMapOf<String, Any>(
        "fillColor" to Color.RED,
        "strokeColor" to Color.BLACK,
        "clickable" to true)

    private var bDrawing = false
    private var selectedPolygon: Polygon? = null
    private var tempPolyline: Polyline? = null

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
        googleMap.setOnMarkerDragListener(this)
        googleMap.setOnPolygonClickListener(this)

        //TODO move camera to user's location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(41.712752765580035,-73.75673331320286), 20.0f))

        loadSavedOstZones()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        //Only create the OstZone on clicking the first marker, and if there's >2 markers
        //Only the first marker is full opacity
        if(marker.alpha < 1f || markers.size < 3) return false

        drawOstZoneOnMapAndSave()
        Log.d("drawing polygon", "closing the polygon with " + markers.size + " points")

        for(m in markers) m.remove()
        markers.clear()

        return true
    }

    override fun onMarkerDragStart(marker: Marker) {

    }

    override fun onMarkerDrag(marker: Marker) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
    }

    override fun onMarkerDragEnd(marker: Marker) {
        redrawPolyline()
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
        ostZones[selectedPolygon]?.id?.let { databaseHelper.removePolygon(it) }
        ostZones.remove(selectedPolygon)
        selectedPolygon = null

        showBottomSheetPlaceholder()
        (findViewById<TextView>(R.id.zoneName)).text = ""
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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

    fun toggleDrawingNewZoneClick(view: View){
        val drawNewZoneBtn = findViewById<Button>(R.id.draw_new_zone_btn)
        if(!bDrawing) {
            drawNewZoneBtn.text = getString(R.string.cancel_drawing_zone)
            bDrawing = true
        }else {
            resetDrawing()
        }
    }

    private fun handleMapTap(tappedPoint: LatLng) {
        if(selectedPolygon != null){
            deselectZone()
            return
        }
        if(!bDrawing) return
        val marker = getOstZoneMarker(tappedPoint, markers.isEmpty())
        googleMap.addMarker(marker)?.let { markers.add(it) }
        redrawPolyline()
    }

    private fun redrawPolyline() {
        tempPolyline?.remove()
        tempPolyline = googleMap.addPolyline(PolylineOptions().apply {
            addAll(markers.map { marker -> marker.position })
        })
    }

    private fun deselectZone(){
        selectedPolygon = null
        showBottomSheetPlaceholder()
        (findViewById<TextView>(R.id.zoneName)).text = ""
    }

    private fun getOstZoneMarker(tappedPoint: LatLng, bFirstMarker: Boolean): MarkerOptions {
        return MarkerOptions().apply{
            position(tappedPoint)
            draggable(true)
            if(!bFirstMarker) alpha(0.25f)
        }
    }

    private fun drawOstZoneOnMapAndSave(){
        val points = markers.map { marker -> marker.position }
        val polygon = googleMap.addPolygon(Utilities.createPolygonFromPoints(points, polygonOptions))
        val ostZone = databaseHelper.saveOstZone(
            OstZone(points, polygonOptions, "The Circle")
        )
        ostZones[polygon] = ostZone
        resetDrawing()
    }

    private fun loadOstZoneToMap(ostZone: OstZone){
        val polygon = googleMap.addPolygon(
            Utilities.createPolygonFromPoints(ostZone.polygonPoints, ostZone.polygonOptions)
        )
        ostZones[polygon] = ostZone
        resetDrawing()
    }

    private fun resetDrawing() {
        bDrawing = false
        tempPolyline?.remove()
        tempPolyline = null
        for(m in markers) m.remove()
        markers.clear()
        findViewById<Button>(R.id.draw_new_zone_btn).text = getString(R.string.draw_new_zone)
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

    private fun loadSavedOstZones(){
        //try{
            for(ostZone in databaseHelper.getAllOstZones()){
                loadOstZoneToMap(ostZone)
            }
        /*}catch (e: Exception){
            Toast.makeText(this, "Failed to load saved OST Zones", Toast.LENGTH_SHORT).show()
        }*/
    }
}