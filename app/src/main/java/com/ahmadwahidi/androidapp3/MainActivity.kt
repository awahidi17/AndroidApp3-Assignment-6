package com.ahmadwahidi.androidapp3

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ahmadwahidi.androidapp3.data.AppDatabase
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.ahmadwahidi.androidapp3.data.TreasureRepository
import com.ahmadwahidi.androidapp3.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Main map screen for the sequential Windsor treasure hunt. */
class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TreasureRepository
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private var currentPlace: TreasurePlace? = null
    private var completionDialogShown = false
    private var verifyVisitAfterPermission = false

    // The map remains browsable without permission; completing a stop requires a live location.
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                enableMyLocation()
                if (verifyVisitAfterPermission) {
                    verifyVisitAfterPermission = false
                    verifyCurrentVisit()
                }
            } else {
                verifyVisitAfterPermission = false
                restoreVisitButton()
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        binding.navigationView.setNavigationItemSelectedListener(this)
        binding.navigationView.setCheckedItem(R.id.nav_map)

        repository = TreasureRepository(
            AppDatabase.getDatabase(applicationContext).treasurePlaceDao()
        )
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.markVisitedButton.setOnClickListener { verifyCurrentVisit() }
        binding.viewDetailsButton.setOnClickListener {
            currentPlace?.let { openPlaceDetails(it.id) }
        }
        binding.allStopsButton.setOnClickListener {
            startActivity(Intent(this, PlacesActivity::class.java))
        }
        binding.howToPlayButton.setOnClickListener { showHowToPlay() }
        binding.shareProgressButton.setOnClickListener { shareProgress() }

        lifecycleScope.launch {
            repository.seedDatabaseIfNeeded()
            refreshHuntState()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            lifecycleScope.launch { refreshHuntState() }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.vibrant_map_style))
        map.setInfoWindowAdapter(TreasureInfoWindowAdapter(this))

        // Tapping a marker's information window opens that unlocked stop.
        map.setOnInfoWindowClickListener { marker ->
            (marker.tag as? TreasurePlace)?.let { openPlaceDetails(it.id) }
        }

        requestLocationPermissionIfNeeded()
        lifecycleScope.launch { refreshHuntState() }
    }

    /** Reads Room data and redraws progress, current clue, and available map markers. */
    private suspend fun refreshHuntState() {
        val allPlaces = repository.getAllPlaces()
        if (allPlaces.isEmpty()) return

        val visitedCount = repository.getVisitedCount()
        val nextPlace = repository.getCurrentPlace()
        currentPlace = nextPlace

        binding.progressText.text = getString(R.string.progress_format, visitedCount, TOTAL_STOPS)
        binding.progressIndicator.max = TOTAL_STOPS
        binding.progressIndicator.setProgressCompat(visitedCount, true)

        if (nextPlace == null) {
            showCompletedState()
        } else {
            completionDialogShown = false
            binding.currentStopText.text =
                getString(R.string.current_stop_format, nextPlace.huntOrder, nextPlace.name)
            binding.clueText.text = nextPlace.clue
            binding.markVisitedButton.isEnabled = true
            binding.markVisitedButton.setText(R.string.verify_visit)
            binding.viewDetailsButton.isEnabled = true

            if (::map.isInitialized) drawAvailableMarkers(allPlaces, nextPlace)
        }
    }

    /** Shows visited stops plus the current stop; future stops remain secret. */
    private fun drawAvailableMarkers(allPlaces: List<TreasurePlace>, nextPlace: TreasurePlace) {
        map.clear()

        allPlaces.filter { it.isVisited || it.id == nextPlace.id }.forEach { place ->
            val hue = if (place.isVisited) {
                BitmapDescriptorFactory.HUE_AZURE
            } else {
                BitmapDescriptorFactory.HUE_ORANGE
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(place.latitude, place.longitude))
                    .title(getString(R.string.marker_title, place.huntOrder, place.name))
                    .snippet(place.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
            marker?.tag = place
        }

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(nextPlace.latitude, nextPlace.longitude),
                DEFAULT_ZOOM
            )
        )
    }

    /** Requests a fresh device position before allowing the current stop to be completed. */
    private fun verifyCurrentVisit() {
        val place = currentPlace ?: return
        if (!hasLocationPermission()) {
            verifyVisitAfterPermission = true
            requestLocationPermissionIfNeeded()
            return
        }

        binding.markVisitedButton.isEnabled = false
        binding.markVisitedButton.setText(R.string.checking_location)

        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        evaluateVisitLocation(place, location)
                    } else {
                        // A cached position is a useful fallback when a fresh GPS fix is unavailable.
                        locationClient.lastLocation
                            .addOnSuccessListener(this) { lastLocation ->
                                if (lastLocation == null) showLocationUnavailable()
                                else evaluateVisitLocation(place, lastLocation)
                            }
                            .addOnFailureListener(this) { showLocationUnavailable() }
                    }
                }
                .addOnFailureListener(this) { showLocationUnavailable() }
        } catch (_: SecurityException) {
            showLocationUnavailable()
        }
    }

    private fun evaluateVisitLocation(place: TreasurePlace, location: Location) {
        if (currentPlace?.id != place.id) return

        val distance = LocationTools.distanceMeters(
            location.latitude,
            location.longitude,
            place.latitude,
            place.longitude
        )
        val distanceLabel = formatDistance(distance)
        restoreVisitButton()

        if (LocationTools.isWithinVisitRadius(distance)) {
            confirmVerifiedVisit(place, distanceLabel)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.move_closer_title)
                .setMessage(
                    getString(
                        R.string.move_closer_message,
                        distanceLabel,
                        LocationTools.VISIT_RADIUS_METERS.roundToInt()
                    )
                )
                .setPositiveButton(R.string.open_in_maps) { _, _ -> openInMaps(place) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /** Saves only a proximity-verified visit and then reveals the next location. */
    private fun confirmVerifiedVisit(place: TreasurePlace, distanceLabel: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_visit_title)
            .setMessage(getString(R.string.confirm_verified_visit_message, place.name, distanceLabel))
            .setPositiveButton(R.string.mark_visited) { _, _ ->
                lifecycleScope.launch {
                    repository.markVisited(place.id)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.stop_completed, place.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshHuntState()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLocationUnavailable() {
        restoreVisitButton()
        Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun restoreVisitButton() {
        if (currentPlace != null) {
            binding.markVisitedButton.isEnabled = true
            binding.markVisitedButton.setText(R.string.verify_visit)
        }
    }

    private fun showCompletedState() {
        binding.currentStopText.setText(R.string.hunt_complete)
        binding.clueText.setText(R.string.completion_message)
        binding.markVisitedButton.isEnabled = false
        binding.markVisitedButton.setText(R.string.hunt_complete_button)
        binding.viewDetailsButton.isEnabled = false
        if (::map.isInitialized) map.clear()

        if (!completionDialogShown) {
            completionDialogShown = true
            AlertDialog.Builder(this)
                .setTitle(R.string.complete_dialog_title)
                .setMessage(R.string.completion_message)
                .setPositiveButton(R.string.share_completion) { _, _ -> shareProgress() }
                .setNegativeButton(R.string.done, null)
                .show()
        }
    }

    private fun openPlaceDetails(placeId: Int) {
        startActivity(
            Intent(this, PlaceDetailActivity::class.java)
                .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, placeId)
        )
    }

    private fun requestLocationPermissionIfNeeded() {
        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (hasLocationPermission()) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map -> Unit
            R.id.nav_places -> startActivity(Intent(this, PlacesActivity::class.java))
            R.id.nav_how_to_play -> showHowToPlay()
            R.id.nav_share -> shareProgress()
            R.id.nav_reset -> confirmReset()
        }
        binding.drawerLayout.closeDrawers()
        return true
    }

    private fun showHowToPlay() {
        AlertDialog.Builder(this)
            .setTitle(R.string.how_to_play_title)
            .setMessage(R.string.how_to_play_message)
            .setPositiveButton(R.string.got_it, null)
            .show()
    }

    /** Shares a plain-text progress card without exposing locked business names. */
    private fun shareProgress() {
        lifecycleScope.launch {
            val visitedCount = repository.getVisitedCount()
            val message = if (visitedCount == TOTAL_STOPS) {
                getString(R.string.share_complete_text)
            } else {
                getString(R.string.share_progress_text, visitedCount, TOTAL_STOPS)
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser)))
        }
    }

    private fun openInMaps(place: TreasurePlace) {
        val query = Uri.encode("${place.latitude},${place.longitude}(${place.name})")
        val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=$query")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_LONG).show()
        }
    }

    private fun formatDistance(distanceMeters: Double): String =
        if (distanceMeters < 1_000) {
            getString(R.string.distance_metres, distanceMeters.roundToInt())
        } else {
            getString(R.string.distance_kilometres, distanceMeters / 1_000.0)
        }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_title)
            .setMessage(R.string.reset_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                lifecycleScope.launch {
                    repository.resetProgress()
                    completionDialogShown = false
                    refreshHuntState()
                    Toast.makeText(this@MainActivity, R.string.progress_reset, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val TOTAL_STOPS = 20
        private const val DEFAULT_ZOOM = 15f
    }
}
