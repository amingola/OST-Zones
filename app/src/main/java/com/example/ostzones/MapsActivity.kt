package com.example.ostzones

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ostzones.api.ApiService
import com.example.ostzones.api.ApiServiceFactory
import com.example.ostzones.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

private const val FILL_COLOR_KEY = "fillColor"
private const val CHECK_LOCATION_TASK_FREQUENCY = 10000L //milliseconds
private const val DEFAULT_ZOOM = 18f
private const val LOCATION_PERMISSION_REQUEST_CODE = 1
private const val PLAYLIST_ACTIVITY_REQUEST_CODE = 1000

class MapsActivity : AppCompatActivity(),
    OnMapClickListener,
    OnMapReadyCallback,
    OnMarkerClickListener,
    OnMarkerDragListener,
    OnPolygonClickListener,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val logTag = "MapsActivity"
    private val polygonsToOstZones: HashMap<Polygon, OstZone> = hashMapOf()
    private val markersToMarkerOptions: MutableMap<Marker, MarkerOptions> = mutableMapOf()
    private val databaseHelper = DatabaseHelper(this)
    private val polygonOptions = hashMapOf<String, Any>(
        "fillColor" to Color.RED,
        "strokeColor" to Color.BLACK,
        "clickable" to true
    )
    private val redirectUri = "com.example.ostzones://login"
    private val scopes = arrayOf(
        "user-read-private",
        "streaming",
        "user-modify-playback-state",
        "playlist-read-private",
        "playlist-read-collaborative"
    )

    private var bDrawing = false
    private var bEditing = false
    private var selectedPolygon: Polygon? = null
    private var idOfOstZonePlayingMusic: Long = 0L
    private var polylineForPolygonBeingEdited: Polyline? = null
    private var centroidMarker: Marker? = null
    private var centroidMarkerStartPos: LatLng? = null
    private var spotifyAppRemote: SpotifyAppRemote? = null //TODO move this to a factory
    private var freehandPoints: MutableSet<LatLng> = mutableSetOf()

    private lateinit var apiService: ApiService
    private lateinit var binding: ActivityMapsBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var scheduledExecutor: ScheduledExecutorService
    private lateinit var handler: Handler
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
        if(savedInstanceState != null){
            idOfOstZonePlayingMusic = savedInstanceState.getLong("zonePlayingMusic", 0L)
        }

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        handler = Handler(Looper.getMainLooper())

        initBottomSheet()
        initEditZoneName()
        initOstZoneColorSliders()

        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        ).setScopes(scopes).build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("zonePlayingMusic", idOfOstZonePlayingMusic)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapClickListener(this)
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMarkerDragListener(this)
        googleMap.setOnPolygonClickListener(this)
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
//        googleMap.setOnCameraMoveStartedListener(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        enableMyLocation()
        centerMapOnUserLocation()

        //ostZones can't be loaded in onCreate(), because the keys are each of their polygons
        //(which can't be saved as a property because they can't be serialized) and they can't be
        //created until the map is available...
        loadSavedOstZones()
        initOstZoneRecyclerView()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PLAYLIST_ACTIVITY_REQUEST_CODE){
            handlePlaylistActivityFinish(resultCode, data)
        }else if(requestCode == com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE){
            handleSpotifyLoginFinish(resultCode, data)
        }
    }

    private fun handleSpotifyLoginFinish(resultCode: Int, data: Intent?) {
        val response = AuthorizationClient.getResponse(resultCode, data)

        if (response.type == AuthorizationResponse.Type.TOKEN) {
            lifecycleScope.launch {
                try {
                    handleSuccessfulLogin(response)
                } catch (e: Exception) {
                    Log.e(logTag, e.message!!)
                }
            }
        } else if (response.type == AuthorizationResponse.Type.ERROR) {
            Utils.longToast(this, response.error)
        }
    }

    private fun handlePlaylistActivityFinish(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            selectedZone()?.playlistUris = data?.getStringArrayListExtra("uris")
            updateOstZone(selectedZone()!!)
        }
    }

    private fun handleSuccessfulLogin(response: AuthorizationResponse) {
        initializeSpotifyAppRemote()
        startCheckWhenUserIsInOstZoneTask()

        val token = response.accessToken
        apiService = ApiServiceFactory.getApiService(token)
    }

    private fun initializeSpotifyAppRemote() {
        val connectionParams = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@MapsActivity.spotifyAppRemote = spotifyAppRemote
                    Log.d(logTag, "Connected to Spotify")
                }

                override fun onFailure(throwable: Throwable){
                    when (throwable) {
                        is CouldNotFindSpotifyApp -> {
                            Utils.longToast(this@MapsActivity,
                                getString(R.string.spotify_not_installed_warning))
                            Log.e(logTag, throwable.message!!)
                        }else ->{
                        Utils.longToast(this@MapsActivity,
                            getString(R.string.spotify_failed_to_connect_warning))
                        }
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor.shutdown()
        handler.removeCallbacks(checkUserIsInOstZoneTask)
    }

    override fun onMapClick(tappedPoint: LatLng) {
        if(selectedPolygon != null && !bEditing){
            deselectZone()
            return
        }
        if(!bDrawing) return
        drawMarkerOnMap(tappedPoint)
        redrawPolyline()
    }

    override fun onMyLocationButtonClick(): Boolean {
        Utils.toast(this, "MyLocation button clicked")
        return false //Return false to not consume the event/animate to the user's current position
    }

    override fun onMyLocationClick(location: Location) =
        Utils.toast(this, "Current location:\n$location")

    override fun onMarkerClick(marker: Marker): Boolean {
        //Only create the OstZone on clicking the first marker, only the first marker is full opacity
        if(marker.alpha < 1f) return false
        if(markersToMarkerOptions.size < 3){
            Utils.toast(this, getString(R.string.minimum_zone_points_warning))
            return false
        }

        drawOstZoneOnMapAndSave(selectedZone())
        for (m in markersToMarkerOptions) m.key.remove()
        markersToMarkerOptions.clear()

        return true
    }

    override fun onMarkerDragStart(marker: Marker) {
        if(marker == centroidMarker) centroidMarkerStartPos = marker.position
    }

    override fun onMarkerDrag(marker: Marker) {
        //googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
        Log.d("drag", marker.position.toString())
        freehandPoints.add(marker.position)
    }

    override fun onMarkerDragEnd(marker: Marker) {
        if(marker == centroidMarker){
            translateSelectedPolygonWithCentroidMarker(marker)
        }
        redrawPolyline()
        drawCentroidMarkerForPolylineToMap()

        googleMap.addPolygon(PolygonOptions().apply{
            addAll(freehandPoints)
            fillColor(Color.RED)
            strokeColor(Color.BLACK)
        })
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun hasCoarseLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun toggleDrawingNewZoneClick(view: View){
        val drawNewZoneBtn = findViewById<Button>(R.id.draw_new_zone_btn)
        if(!bDrawing) {
            drawNewZoneBtn.text = getString(R.string.cancel_drawing_zone)
            bDrawing = true
        }else {
            resetDrawing()
        }
    }

    fun deleteSelectedZoneClick(view: View) = deleteSelectedZone()

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

    fun updateSelectedPolygonColorOnMap() {
        val alpha = ostZoneColorAlphaSeekBar.progress
        val red = ostZoneColorRedSeekBar.progress
        val green = ostZoneColorGreenSeekBar.progress
        val blue = ostZoneColorBlueSeekBar.progress
        val color = Color.argb(alpha, red, green, blue)

        selectedPolygon?.fillColor = color
        selectedZone()?.let { it.polygonOptions[FILL_COLOR_KEY] = color }
    }

    fun updateSelectedPolygonColorInDatabase(){
        selectedZone()?.let { updateOstZone(it) }
    }

    fun zonesNavClick(item: MenuItem) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        showBottomSheetPlaceholder()
    }

    fun ostsNavClick(item: MenuItem) {
        if(selectedZone() == null){
            val warningMessage = if(polygonsToOstZones.isEmpty())
                getString(R.string.ost_nav_warning_no_ost_zones)
                else getString(R.string.ost_nav_warning_no_selected_ost_zone)
            Utils.toast(this, warningMessage)
        }else{
            val intent = Intent(this, PlaylistActivity::class.java)
            val selectedUris = selectedZone()?.playlistUris?.toCollection(ArrayList())
            intent.putStringArrayListExtra("selectedUris", selectedUris)
            startActivityForResult(intent, PLAYLIST_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun selectedZone() = polygonsToOstZones[selectedPolygon]

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val centerLocationButton = findViewById<FloatingActionButton>(R.id.center_location_btn)
        centerLocationButton.setOnClickListener { centerMapOnUserLocation() }

        if (hasFineLocationPermission() || hasCoarseLocationPermission()) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun centerMapOnUserLocation() {
        if (hasFineLocationPermission() || hasCoarseLocationPermission()) {
            getUserLatLng()?.let {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM))
            }
        }
    }

    private fun drawOstZoneOnMapAndSave(existingOstZone: OstZone?){
        val points = markersToMarkerOptions.map { entry -> entry.key.position }
        val polygon = googleMap.addPolygon(Utils.createPolygonOptions(points, polygonOptions))

        if(existingOstZone == null)
            createNewOstZone(points, polygon)
        else
            overwriteExistingOstZone(existingOstZone, points, polygon)


        initOstZoneRecyclerView()
        resetDrawing()
        onPolygonClick(polygon)
    }

    private fun createNewOstZone(points: List<LatLng>, polygon: Polygon) {
        val ostZone = databaseHelper.saveOstZone(OstZone("", points, polygonOptions))
        polygonsToOstZones[polygon] = ostZone
    }

    private fun overwriteExistingOstZone(existingOstZone: OstZone, points: List<LatLng>, polygon: Polygon) {
        existingOstZone.polygonPoints = points
        existingOstZone.polygonOptions = polygonOptions
        databaseHelper.updateOstZone(existingOstZone)

        polygonsToOstZones.remove(selectedPolygon)
        polygonsToOstZones[polygon] = existingOstZone

        removeSelectedZoneFromMap()
    }

    private fun updateOstZone(ostZone: OstZone){
        databaseHelper.updateOstZone(ostZone)
        initOstZoneRecyclerView()
    }

    private fun loadOstZoneToMap(ostZone: OstZone){
        val polygonOptions = Utils.createPolygonOptions(ostZone.polygonPoints, ostZone.polygonOptions)
        val polygon = googleMap.addPolygon(polygonOptions)
        polygonsToOstZones[polygon] = ostZone
        resetDrawing()
    }

    private fun drawMarkerOnMap(tappedPoint: LatLng) {
        val markerOptions = Utils.getOstZoneMarkerOptions(tappedPoint, markersToMarkerOptions.isEmpty())
        googleMap.addMarker(markerOptions)?.let { markersToMarkerOptions[it] = markerOptions }
    }

    private fun drawCentroidMarkerForSelectedPolygonToMap() {
        centroidMarker?.remove()
        val centroidPoint = Utils.computeCentroidOfSelectedPolygon(selectedPolygon!!)
        val centroidMarkerOptions = Utils.getCentroidMarkerOptions(centroidPoint)
        centroidMarker = googleMap.addMarker(centroidMarkerOptions)
    }

    private fun drawCentroidMarkerForPolylineToMap() {
        centroidMarker?.remove()
        val centroidPoint = Utils.computeCentroidOfPoints(polylineForPolygonBeingEdited!!.points)
        val centroidMarkerOptions = Utils.getCentroidMarkerOptions(centroidPoint)
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
            val newPosition = Utils.calculateNewPosition(centroidMarkerStartPos!!, centroidMarker.position, polygonMarker.position)
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
            val bPressedEnter = event != null
                        && event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER

            if (actionId == EditorInfo.IME_ACTION_DONE || bPressedEnter) {
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

        ostZoneColorRedSeekBar.setOnSeekBarChangeListener(RgbSeekBarOnChangeListener(this, ostZoneColorRedSeekBarLabel))
        ostZoneColorGreenSeekBar.setOnSeekBarChangeListener(RgbSeekBarOnChangeListener(this, ostZoneColorGreenSeekBarLabel))
        ostZoneColorBlueSeekBar.setOnSeekBarChangeListener(RgbSeekBarOnChangeListener(this, ostZoneColorBlueSeekBarLabel))
        ostZoneColorAlphaSeekBar.setOnSeekBarChangeListener(RgbSeekBarOnChangeListener(this, ostZoneColorAlphaSeekBarLabel))
    }

    private fun initOstZoneRecyclerView() {
        val listAdapter = OstZoneListAdapter(this, polygonsToOstZones)
        listAdapter.onItemClick = { ostZone ->
            val centroid = Utils.computeCentroidOfPoints(ostZone.polygonPoints)
            val selectedPolygon = polygonsToOstZones.filterValues { it == ostZone }.keys.first()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centroid, DEFAULT_ZOOM))
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
            val color = it.polygonOptions[FILL_COLOR_KEY] as Int

            val red = Utils.getColorComponentDecimalValue(color, SeekBarColor.RED)
            val green = Utils.getColorComponentDecimalValue(color, SeekBarColor.GREEN)
            val blue = Utils.getColorComponentDecimalValue(color, SeekBarColor.BLUE)
            val alpha = Utils.getColorComponentDecimalValue(color, SeekBarColor.ALPHA)

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
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun startCheckWhenUserIsInOstZoneTask() =
        handler.postDelayed(checkUserIsInOstZoneTask, CHECK_LOCATION_TASK_FREQUENCY)

    private val checkUserIsInOstZoneTask = checkUserIsInOstZoneTask()

    private fun checkUserIsInOstZoneTask() = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (hasFineLocationPermission() || hasCoarseLocationPermission()) {
                val occupiedOstZone = getUserLatLng()?.let {
                    polygonsToOstZones.filterValues { ostZone -> ostZone.isPointInside(it) }
                        .values.firstOrNull()
                }

                Log.d("location check", "User is inside ${occupiedOstZone?.name}")

                if(occupiedOstZone != null && occupiedOstZone.id != idOfOstZonePlayingMusic){
                    idOfOstZonePlayingMusic = Utils.playRandom(spotifyAppRemote?.playerApi, occupiedOstZone)!!
                }else if(occupiedOstZone == null){
                    spotifyAppRemote?.playerApi?.pause()
                }
            }
            handler.postDelayed(this, CHECK_LOCATION_TASK_FREQUENCY)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLatLng(): LatLng? {
        return try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: UninitializedPropertyAccessException) {
            null //Just eat this; locationManager init is hit-or-miss :/
        }
    }
}