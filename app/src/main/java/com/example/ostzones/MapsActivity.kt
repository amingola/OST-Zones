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
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener,
    OnMarkerDragListener, OnPolygonClickListener{

    private val ostZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markers: ArrayList<Marker> = arrayListOf()
    private val databaseHelper = DatabaseHelper(this)
    private val polygonOptions = hashMapOf<String, Any>(
        "fillColor" to Color.RED,
        "strokeColor" to Color.BLACK,
        "clickable" to true)

    private var bDrawing = false
    private var bEditing = false
    private var selectedPolygon: Polygon? = null
    private var tempPolyline: Polyline? = null
    private var centroidMarker: Marker? = null

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
        //Only create the OstZone on clicking the first marker, only the first marker is full opacity
        if(marker.alpha < 1f) return false
        if(markers.size < 3){
            Toast.makeText(this, getString(R.string.minimum_zone_points_warning), Toast.LENGTH_LONG).show()
            return false
        }

        drawOstZoneOnMapAndSave(selectedZone())
        for (m in markers) m.remove()
        markers.clear()

        return true
    }

    override fun onMarkerDragStart(marker: Marker) {

    }

    override fun onMarkerDrag(marker: Marker) {
        //googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
    }

    override fun onMarkerDragEnd(marker: Marker) {
        redrawPolyline()
    }

    override fun onPolygonClick(polygon: Polygon) {
        if(bDrawing) return

        selectedPolygon = polygon
        val ostZone = ostZones[polygon]
        Log.d("polygon", "just clicked a polygon named: " + ostZone?.name)

        if(ostZone != null) (findViewById<TextView>(R.id.zoneName)).text = ostZone.name
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        showBottomSheetEditFunctionality()
    }

    fun deleteSelectedZoneClick(view: View) {
        deleteSelectedZone()
    }

    fun toggleEditingSelectedZone(view: View) {
        if(!bEditing){
            bDrawing = true
            bEditing = true

            (findViewById<TextView>(R.id.edit_selected_zone_btn)).text = getString(R.string.cancel_editing)

            val points = selectedPolygon?.points!!.toMutableList().apply{ removeLast() }
            for(point in points){
                drawMarkerOnMap(point)
            }

            val centroidPoint = Utilities.getCentroidPointOfSelectedPolygon(selectedPolygon!!)
            val centroidMarkerOptions = Utilities.getCentroidMarkerOptions(centroidPoint, markers.isEmpty())
            centroidMarker = googleMap.addMarker(centroidMarkerOptions)
        }else{
            resetDrawing()
        }
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
        if(selectedPolygon != null && !bEditing){
            deselectZone()
            return
        }
        if(!bDrawing) return
        drawMarkerOnMap(tappedPoint)
        redrawPolyline()
    }

    private fun drawOstZoneOnMapAndSave(existingOstZone: OstZone?){
        val points = markers.map { marker -> marker.position }
        val polygon = googleMap.addPolygon(Utilities.createPolygonOptions(points, polygonOptions))

        //Create a new OST Zone
        if(existingOstZone == null) {
            val ostZone = databaseHelper.saveOstZone(
                OstZone(points, polygonOptions, "The Circle")
            )
            ostZones[polygon] = ostZone

        //Overwrite existing OST Zone
        }else{
            existingOstZone.polygonPoints = points
            existingOstZone.polygonOptions = polygonOptions
            databaseHelper.updateOstZone(existingOstZone)

            ostZones.remove(selectedPolygon)
            ostZones[polygon] = existingOstZone

            removeSelectedZoneFromMap()
        }
        resetDrawing()
    }

    private fun loadOstZoneToMap(ostZone: OstZone){
        val polygon = googleMap.addPolygon(
            Utilities.createPolygonOptions(ostZone.polygonPoints, ostZone.polygonOptions)
        )
        ostZones[polygon] = ostZone
        resetDrawing()
    }

    private fun drawMarkerOnMap(tappedPoint: LatLng) {
        val marker = Utilities.getOstZoneMarkerOptions(tappedPoint, markers.isEmpty())
        googleMap.addMarker(marker)?.let { markers.add(it) }
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

    private fun resetDrawing() {
        bDrawing = false
        bEditing = false

        tempPolyline?.remove()
        tempPolyline = null

        for(m in markers) m.remove()
        markers.clear()

        centroidMarker?.remove()
        centroidMarker = null

        findViewById<Button>(R.id.draw_new_zone_btn).text = getString(R.string.draw_new_zone)
        (findViewById<TextView>(R.id.edit_selected_zone_btn)).text = getString(R.string.edit_selected_zone)
    }

    private fun deleteSelectedZone() {
        ostZones[selectedPolygon]?.id?.let { databaseHelper.removePolygon(it) }
        removeSelectedZoneFromMap()
    }

    private fun removeSelectedZoneFromMap() {
        selectedPolygon?.remove()
        ostZones.remove(selectedPolygon)
        selectedPolygon = null

        showBottomSheetPlaceholder()
        (findViewById<TextView>(R.id.zoneName)).text = ""
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