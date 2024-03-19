package com.example.ostzones

import DatabaseHelper
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener,
    OnMarkerDragListener, OnPolygonClickListener{

    private val polygonsToOstZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markersToMarkerOptions: MutableMap<Marker, MarkerOptions> = mutableMapOf()
    private val databaseHelper = DatabaseHelper(this)
    private val polygonOptions = hashMapOf<String, Any>(
        "fillColor" to Color.RED,
        "strokeColor" to Color.BLACK,
        "clickable" to true
    )

    private var bDrawing = false
    private var bEditing = false
    private var selectedPolygon: Polygon? = null
    private var polylineForPolygonBeingEdited: Polyline? = null
    private var centroidMarker: Marker? = null
    private var centroidMarkerStartPos: LatLng? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var zoneNameEditText: EditText
    private lateinit var ostZonesRecyclerView: RecyclerView
    private lateinit var ostZoneColorRedSeekBarLabel: TextView
    private lateinit var ostZoneColorGreenSeekBarLabel: TextView
    private lateinit var ostZoneColorBlueSeekBarLabel: TextView
    private lateinit var ostZoneColorAlphaSeekBarLabel: TextView
    private lateinit var ostZoneColorRedSeekBar: SeekBar
    private lateinit var ostZoneColorGreenSeekBar: SeekBar
    private lateinit var ostZoneColorBlueSeekBar: SeekBar
    private lateinit var ostZoneColorAlphaSeekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initBottomSheet()
        initEditZoneName()
        initOstZoneColorSliders()
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

        //ostZones can't be loaded in onCreate(), because the keys are each of their polygons
        //(which can't be saved as a property because they can't be serialized) and they can't be
        //created until the map is available...
        loadSavedOstZones()
        initOstZoneRecyclerView()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        //Only create the OstZone on clicking the first marker, only the first marker is full opacity
        if(marker.alpha < 1f) return false
        if(markersToMarkerOptions.size < 3){
            Toast.makeText(this, getString(R.string.minimum_zone_points_warning), Toast.LENGTH_LONG).show()
            return false
        }

        drawOstZoneOnMapAndSave(selectedZone())
        for (m in markersToMarkerOptions) m.key.remove()
        markersToMarkerOptions.clear()

        return true
    }

    override fun onMarkerDragStart(marker: Marker) {
        if(marker == centroidMarker){
            centroidMarkerStartPos = marker.position
        }
    }

    override fun onMarkerDrag(marker: Marker) {
        //googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
    }

    override fun onMarkerDragEnd(marker: Marker) {
        if(marker == centroidMarker){
            translateSelectedPolygonWithCentroidMarker(marker)
        }
        redrawPolyline()
        drawCentroidMarkerForPolylineToMap()
    }

    override fun onPolygonClick(polygon: Polygon) {
        if(bDrawing) return

        selectedPolygon = polygon
        val ostZone = polygonsToOstZones[polygon]

        if(ostZone != null) zoneNameEditText.setText(ostZone.name)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        showBottomSheetEditFunctionality()
        updatePolygonArgbComponents()
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

            drawCentroidMarkerForSelectedPolygonToMap()
        }else{
            resetDrawing()
        }
    }

    fun zonesNavClick(item: MenuItem) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        showBottomSheetPlaceholder()
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

    private fun selectedZone() = polygonsToOstZones[selectedPolygon]

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
        val points = markersToMarkerOptions.map { entry -> entry.key.position }
        val polygon = googleMap.addPolygon(Utilities.createPolygonOptions(points, polygonOptions))

        //Create a new OST Zone
        if(existingOstZone == null) {
            val ostZone = databaseHelper.saveOstZone(
                OstZone(points, polygonOptions, "")
            )
            polygonsToOstZones[polygon] = ostZone

        //Overwrite existing OST Zone
        }else{
            existingOstZone.polygonPoints = points
            existingOstZone.polygonOptions = polygonOptions
            databaseHelper.updateOstZone(existingOstZone)

            polygonsToOstZones.remove(selectedPolygon)
            polygonsToOstZones[polygon] = existingOstZone

            removeSelectedZoneFromMap()
        }
        initOstZoneRecyclerView()
        resetDrawing()
        onPolygonClick(polygon)
    }

    private fun updateOstZone(ostZone: OstZone){
        databaseHelper.updateOstZone(ostZone)
        initOstZoneRecyclerView()
    }

    private fun loadOstZoneToMap(ostZone: OstZone){
        val polygon = googleMap.addPolygon(
            Utilities.createPolygonOptions(ostZone.polygonPoints, ostZone.polygonOptions)
        )
        polygonsToOstZones[polygon] = ostZone
        resetDrawing()
    }

    private fun drawMarkerOnMap(tappedPoint: LatLng) {
        val markerOptions = Utilities.getOstZoneMarkerOptions(tappedPoint, markersToMarkerOptions.isEmpty())
        markersToMarkerOptions
        googleMap.addMarker(markerOptions)?.let { markersToMarkerOptions[it] = markerOptions }
    }

    private fun drawCentroidMarkerForSelectedPolygonToMap() {
        centroidMarker?.remove()
        val centroidPoint = Utilities.computeCentroidOfSelectedPolygon(selectedPolygon!!)
        val centroidMarkerOptions = Utilities.getCentroidMarkerOptions(centroidPoint)
        centroidMarker = googleMap.addMarker(centroidMarkerOptions)
    }

    private fun drawCentroidMarkerForPolylineToMap() {
        centroidMarker?.remove()
        val centroidPoint = Utilities.computeCentroidOfPoints(polylineForPolygonBeingEdited!!.points)
        val centroidMarkerOptions = Utilities.getCentroidMarkerOptions(centroidPoint)
        centroidMarker = googleMap.addMarker(centroidMarkerOptions)
    }

    private fun redrawPolyline() {
        polylineForPolygonBeingEdited?.remove()
        polylineForPolygonBeingEdited = googleMap.addPolyline(PolylineOptions().apply {
            addAll(markersToMarkerOptions.map { entry -> entry.key.position })
        })
    }

    private fun deselectZone(){
        selectedPolygon = null
        showBottomSheetPlaceholder()
        zoneNameEditText.setText("")
    }

    private fun resetDrawing() {
        bDrawing = false
        bEditing = false

        polylineForPolygonBeingEdited?.remove()
        polylineForPolygonBeingEdited = null

        for(m in markersToMarkerOptions) m.key.remove()
        markersToMarkerOptions.clear()

        centroidMarker?.remove()
        centroidMarker = null

        findViewById<Button>(R.id.draw_new_zone_btn).text = getString(R.string.draw_new_zone)
        (findViewById<TextView>(R.id.edit_selected_zone_btn)).text = getString(R.string.edit_selected_zone)
    }

    private fun deleteSelectedZone() {
        polygonsToOstZones[selectedPolygon]?.id?.let { databaseHelper.removePolygon(it) }
        removeSelectedZoneFromMap()
    }

    private fun removeSelectedZoneFromMap() {
        selectedPolygon?.remove()
        polygonsToOstZones.remove(selectedPolygon)
        selectedPolygon = null

        showBottomSheetPlaceholder()
        zoneNameEditText.setText("")
    }

    private fun translateSelectedPolygonWithCentroidMarker(centroidMarker: Marker) {
        val newMarkers: MutableMap<Marker, MarkerOptions> = mutableMapOf()

        for(polygonMarker in markersToMarkerOptions.keys){
            val newPosition = Utilities.calculateNewPosition(centroidMarkerStartPos!!, centroidMarker.position, polygonMarker.position)
            val markerOptions = markersToMarkerOptions[polygonMarker]!!.position(newPosition)
            val newMarker = googleMap.addMarker(markerOptions)

            polygonMarker.remove()
            newMarkers[newMarker!!] = markerOptions
        }

        markersToMarkerOptions.clear()
        markersToMarkerOptions.putAll(newMarkers)
    }

    private fun loadSavedOstZones(){
        for(ostZone in databaseHelper.getAllOstZones()){
            loadOstZoneToMap(ostZone)
        }
    }

    private fun showBottomSheetEditFunctionality() {
        findViewById<View>(R.id.bottom_sheet_placeholder).visibility = View.GONE
        findViewById<View>(R.id.bottom_sheet_functionality).visibility = View.VISIBLE
    }

    private fun showBottomSheetPlaceholder() {
        findViewById<View>(R.id.bottom_sheet_functionality).visibility = View.GONE
        findViewById<View>(R.id.bottom_sheet_placeholder).visibility = View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initBottomSheet() {
        bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.setOnTouchListener { _, _ -> true } //Eat taps so the map doesn't get them
    }

    private fun initEditZoneName() {
        zoneNameEditText = findViewById(R.id.zone_name_edit_text)
        zoneNameEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                event != null && event.action == KeyEvent.ACTION_DOWN &&
                event.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                hideKeyboard(zoneNameEditText)
                selectedZone()?.let {
                    it.name = zoneNameEditText.text.toString()
                    updateOstZone(it)
                }

                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun initOstZoneColorSliders() {
        ostZoneColorRedSeekBarLabel = findViewById(R.id.polygon_options_slider_progress_label_red)
        ostZoneColorGreenSeekBarLabel = findViewById(R.id.polygon_options_slider_progress_label_green)
        ostZoneColorBlueSeekBarLabel = findViewById(R.id.polygon_options_slider_progress_label_blue)
        ostZoneColorAlphaSeekBarLabel = findViewById(R.id.polygon_options_slider_progress_label_alpha)

        ostZoneColorRedSeekBar = findViewById(R.id.polygon_options_slider_red)
        ostZoneColorGreenSeekBar = findViewById(R.id.polygon_options_slider_green)
        ostZoneColorBlueSeekBar = findViewById(R.id.polygon_options_slider_blue)
        ostZoneColorAlphaSeekBar = findViewById(R.id.polygon_options_slider_alpha)

        ostZoneColorRedSeekBar.setOnSeekBarChangeListener(
            RgbSeekBarOnChangeListener(this, ostZoneColorRedSeekBarLabel)
        )
        ostZoneColorGreenSeekBar.setOnSeekBarChangeListener(
            RgbSeekBarOnChangeListener(this, ostZoneColorGreenSeekBarLabel)
        )
        ostZoneColorBlueSeekBar.setOnSeekBarChangeListener(
            RgbSeekBarOnChangeListener(this, ostZoneColorBlueSeekBarLabel)
        )
        ostZoneColorAlphaSeekBar.setOnSeekBarChangeListener(
            RgbSeekBarOnChangeListener(this, ostZoneColorAlphaSeekBarLabel)
        )
    }

    private fun initOstZoneRecyclerView() {
        val listAdapter = OstZoneListAdapter(this, polygonsToOstZones)
        listAdapter.onItemClick = { ostZone ->
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    Utilities.computeCentroidOfPoints(ostZone.polygonPoints), 20.0f
                )
            )
            val selectedPolygon = polygonsToOstZones.filterValues { it == ostZone }.keys.first()
            onPolygonClick(selectedPolygon)
        }
        ostZonesRecyclerView = findViewById(R.id.ost_zones_recycler_view)
        ostZonesRecyclerView.also {
            it.adapter = listAdapter
            it.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun updatePolygonArgbComponents() {
        selectedZone()?.let {
            val color = it.polygonOptions["fillColor"] as Int

            val red = Utilities.getColorComponentDecimalValue(color, SeekBarColor.RED)
            val green = Utilities.getColorComponentDecimalValue(color, SeekBarColor.GREEN)
            val blue = Utilities.getColorComponentDecimalValue(color, SeekBarColor.BLUE)
            val alpha = Utilities.getColorComponentDecimalValue(color, SeekBarColor.ALPHA)

            ostZoneColorRedSeekBarLabel.text = red.toString()
            ostZoneColorRedSeekBar.progress = red

            ostZoneColorGreenSeekBarLabel.text = green.toString()
            ostZoneColorGreenSeekBar.progress = green

            ostZoneColorBlueSeekBarLabel.text = blue.toString()
            ostZoneColorBlueSeekBar.progress = blue

            ostZoneColorAlphaSeekBarLabel.text = alpha.toString()
            ostZoneColorAlphaSeekBar.progress = alpha
        }
    }

    private fun hideKeyboard(editText: EditText){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    fun updateSelectedPolygonColor() {
        val alpha = ostZoneColorAlphaSeekBar.progress
        val red = ostZoneColorRedSeekBar.progress
        val green = ostZoneColorGreenSeekBar.progress
        val blue = ostZoneColorBlueSeekBar.progress
        selectedPolygon?.fillColor = Color.argb(alpha, red, green, blue)
    }
}