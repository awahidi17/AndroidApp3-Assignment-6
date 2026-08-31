package com.ahmadwahidi.androidapp3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.MapStyleOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

/** Main map screen for the sequential Windsor treasure hunt. */
class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TreasureRepository
    private lateinit var map: GoogleMap
    private var currentPlace: TreasurePlace? = null
    private var completionDialogShown = false

    // The app works without location access, but the blue user-location layer needs permission.
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                enableMyLocation()
            } else {
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

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.markVisitedButton.setOnClickListener { confirmCurrentVisit() }
        binding.viewDetailsButton.setOnClickListener {
            currentPlace?.let { openPlaceDetails(it.id) }
        }
        binding.allStopsButton.setOnClickListener {
            startActivity(Intent(this, PlacesActivity::class.java))
        }
        binding.howToPlayButton.setOnClickListener { showHowToPlay() }

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

        // Tapping a marker's information window opens that unlocked stop.
        map.setOnInfoWindowClickListener { marker ->
            (marker.tag as? Int)?.let { openPlaceDetails(it) }
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
            marker?.tag = place.id
        }

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(nextPlace.latitude, nextPlace.longitude),
                DEFAULT_ZOOM
            )
        )
    }

    /** Confirms the visit, saves it in Room, and unlocks the next stop. */
    private fun confirmCurrentVisit() {
        val place = currentPlace ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_visit_title)
            .setMessage(getString(R.string.confirm_visit_message, place.name))
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

    private fun showCompletedState() {
        binding.currentStopText.setText(R.string.hunt_complete)
        binding.clueText.setText(R.string.completion_message)
        binding.markVisitedButton.isEnabled = false
        binding.viewDetailsButton.isEnabled = false
        if (::map.isInitialized) map.clear()

        if (!completionDialogShown) {
            completionDialogShown = true
            AlertDialog.Builder(this)
                .setTitle(R.string.complete_dialog_title)
                .setMessage(R.string.completion_message)
                .setPositiveButton(R.string.awesome, null)
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
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
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

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map -> Unit
            R.id.nav_places -> startActivity(Intent(this, PlacesActivity::class.java))
            R.id.nav_how_to_play -> showHowToPlay()
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
