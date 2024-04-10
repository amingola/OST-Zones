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
import com.google.android.gms.maps.model.Polygon
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
private const val CHECK_LOCATION_TASK_FREQUENCY = 1000L //milliseconds
private const val FREEHAND_MARKER_DRAW_FREQUENCY = 10   //every nth marker to be drawn when free-hand drawing
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
    private var polyline: Polyline? = null
    private var freehandMarker: Marker? = null
    private var idOfOstZonePlayingMusic: Long? = 0L
    private var spotifyAppRemote: SpotifyAppRemote? = null //TODO move this to a factory
    private var freehandPoints: MutableSet<LatLng> = mutableSetOf()
    private var freehandMarkerCounter = 0

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
        if(idOfOstZonePlayingMusic != null)
            outState.putLong("zonePlayingMusic", idOfOstZonePlayingMusic!!)
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
                    if (throwable is CouldNotFindSpotifyApp) {
                        Utils.longToast(this@MapsActivity,
                            getString(R.string.spotify_not_installed_warning))
                        Log.e(logTag, throwable.message!!)
                    } else {
                        Utils.longToast(this@MapsActivity,
                            getString(R.string.spotify_failed_to_connect_warning))
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
        if(selectedPolygon != null && !bEditing) deselectZone()

        freehandMarker?.remove()
        if(bDrawing){
            val markerOptions = Utils.getFreehandDrawingMarkerOptions(tappedPoint, resources)
            freehandMarker = googleMap.addMarker(markerOptions)
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Utils.toast(this, "MyLocation button clicked")
        return false //Return false to not consume the event/animate to the user's current position
    }

    override fun onMyLocationClick(location: Location) =
        Utils.toast(this, "Current location:\n$location")

    override fun onMarkerClick(marker: Marker) = true
    override fun onMarkerDragStart(marker: Marker) {}

    override fun onMarkerDrag(marker: Marker) {
        if(freehandMarkerCounter % FREEHAND_MARKER_DRAW_FREQUENCY == 0){
            polyline?.remove()
            freehandPoints.add(marker.position)
            polyline = googleMap.addPolyline(PolylineOptions().addAll(freehandPoints))
        }
        freehandMarkerCounter++
        freehandPoints.add(marker.position)
    }

    override fun onMarkerDragEnd(marker: Marker) {
        freehandMarker?.remove()
        polyline?.remove()
        freehandMarker = null
        drawOstZoneOnMapAndSave(selectedZone())
        resetDrawingState()

        if(bEditing) resetEditingState()
    }

    override fun onPolygonClick(polygon: Polygon) {
        if(bEditing) return //disallow interacting w/ other zones while editing

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
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            drawNewZoneBtn.text = getString(R.string.cancel_drawing_zone)

            startDrawingState()
        }else {
            resetDrawingState()
        }
    }

    fun deleteSelectedZoneClick(view: View) {
        polygonsToOstZones[selectedPolygon]?.id?.let { databaseHelper.removePolygon(it) }
        removeSelectedZoneFromMap()
    }

    fun toggleEditingSelectedZone(view: View) {
        if(!bEditing){
            bEditing = true
            startDrawingState()
            (findViewById<TextView>(R.id.edit_selected_zone_btn)).text = getString(R.string.cancel_editing)

            //Halve the opacity of the selected zone
            //selectedPolygon?.fillColor = halfOpacityOfColor(selectedPolygon?.fillColor!!)
        }else{
            resetDrawingState()
            resetEditingState()
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

    private fun polygonFromZone(existingOstZone: OstZone) =
        polygonsToOstZones.filterValues { it == existingOstZone }.keys.first()

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

    private fun drawOstZoneOnMapAndSave(existingOstZone: OstZone?): OstZone {
        val newPoints = freehandPoints.toList()
        val newPolygon: Polygon?
        val savedOstZone: OstZone?

        if(existingOstZone == null) {
            newPolygon = googleMap.addPolygon(Utils.createPolygonOptions(newPoints, polygonOptions))
            savedOstZone = createNewOstZone(newPoints, newPolygon)
        } else {
            val newPolygonOptions = Utils.createPolygonOptions(newPoints, existingOstZone.polygonOptions)
            newPolygon = googleMap.addPolygon(newPolygonOptions)
            savedOstZone = overwriteExistingOstZone(existingOstZone, newPoints, newPolygon)
        }

        initOstZoneRecyclerView()
        onPolygonClick(newPolygon)
        return savedOstZone
    }

    private fun createNewOstZone(points: List<LatLng>, polygon: Polygon): OstZone {
        val ostZone = databaseHelper.saveOstZone(OstZone("Untitled ${points.size}", points, polygonOptions))
        polygonsToOstZones[polygon] = ostZone
        return ostZone
    }

    private fun overwriteExistingOstZone(existingOstZone: OstZone, newPoints: List<LatLng>, newPolygon: Polygon): OstZone {
        existingOstZone.polygonPoints = newPoints
        val updatedOstZone = databaseHelper.updateOstZone(existingOstZone)

        polygonsToOstZones.remove(polygonFromZone(existingOstZone))
        polygonsToOstZones[newPolygon] = existingOstZone

        removeSelectedZoneFromMap()
        return updatedOstZone
    }

    private fun updateOstZone(ostZone: OstZone){
        databaseHelper.updateOstZone(ostZone)
        initOstZoneRecyclerView()
    }

    private fun deselectZone(){
        selectedPolygon = null
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        zoneNameEditText.setText("")
    }

    private fun removeSelectedZoneFromMap() {
        selectedPolygon?.remove()
        polygonsToOstZones.remove(selectedPolygon)
        selectedPolygon = null

        showBottomSheetPlaceholder()
        zoneNameEditText.setText("")
    }

    private fun loadOstZoneToMap(ostZone: OstZone){
        val polygonOptions = Utils.createPolygonOptions(ostZone.polygonPoints, ostZone.polygonOptions)
        val polygon = googleMap.addPolygon(polygonOptions)
        polygonsToOstZones[polygon] = ostZone
    }

    private fun startDrawingState() {
        bDrawing = true
        Utils.longToast(this, getString(R.string.start_drawing_zone_toast))
    }

    private fun resetDrawingState() {
        bDrawing = false
        freehandPoints.clear()
        findViewById<Button>(R.id.draw_new_zone_btn).text = getString(R.string.draw_new_zone)
    }

    private fun resetEditingState() {
        bEditing = false
        (findViewById<TextView>(R.id.edit_selected_zone_btn)).text =
            getString(R.string.edit_selected_zone)
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
            val selectedPolygon = polygonFromZone(ostZone)
            val zoom = Utils.calculateZoomLevel(resources, selectedPolygon.points)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centroid, zoom))
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

    private fun halfOpacityOfColor(color: Int): Int{
        val a = Utils.getColorComponentDecimalValue(color, SeekBarColor.ALPHA).toFloat() / 2
        val r = Utils.getColorComponentDecimalValue(color, SeekBarColor.RED).toFloat()
        val g = Utils.getColorComponentDecimalValue(color, SeekBarColor.GREEN).toFloat()
        val b = Utils.getColorComponentDecimalValue(color, SeekBarColor.BLUE).toFloat()
        return Color.argb(a, r, g, b)
    }

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
                    idOfOstZonePlayingMusic = Utils.playRandom(spotifyAppRemote?.playerApi, occupiedOstZone)
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