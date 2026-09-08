package com.ahmadwahidi.androidapp3

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ahmadwahidi.androidapp3.data.AppDatabase
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.ahmadwahidi.androidapp3.data.TreasureRepository
import com.ahmadwahidi.androidapp3.databinding.ActivityPlaceDetailBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/** Detail screen with clue, map action, personal notes, and a saved place photo. */
class PlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceDetailBinding
    private lateinit var repository: TreasureRepository
    private lateinit var locationClient: FusedLocationProviderClient
    private var placeId: Int = INVALID_ID
    private var displayedPlace: TreasurePlace? = null
    private var isCurrentStop = false
    private var pendingLocationAction: Boolean? = null
    private var pendingCameraUri: Uri? = null
    private var pendingCameraFile: File? = null

    // OpenDocument lets the app retain access to the selected photo after a restart.
    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers grant access without a persistable flag.
            }

            lifecycleScope.launch {
                savePhoto(uri)
            }
        }

    // TakePicture writes a full-resolution image to the private file supplied by FileProvider.
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pendingCameraUri
            if (saved && uri != null) {
                lifecycleScope.launch { savePhoto(uri) }
            } else {
                pendingCameraFile?.delete()
            }
            pendingCameraUri = null
            pendingCameraFile = null
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val completeWhenNearby = pendingLocationAction
            pendingLocationAction = null

            if (granted && completeWhenNearby != null) {
                checkCurrentDistance(completeWhenNearby)
            } else if (!granted) {
                restoreLocationButtons()
                Toast.makeText(this, R.string.location_required_for_visit, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = TreasureRepository(
            AppDatabase.getDatabase(applicationContext).treasurePlaceDao()
        )
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        placeId = intent.getIntExtra(EXTRA_PLACE_ID, INVALID_ID)
        if (placeId == INVALID_ID) {
            finish()
            return
        }

        pendingCameraUri = savedInstanceState?.getString(STATE_CAMERA_URI)?.let(Uri::parse)
        pendingCameraFile = savedInstanceState?.getString(STATE_CAMERA_FILE)?.let(::File)

        binding.backButton.setOnClickListener { finish() }
        binding.openMapButton.setOnClickListener { displayedPlace?.let(::openInMaps) }
        binding.choosePhotoButton.setOnClickListener { showPhotoOptions() }
        binding.saveNotesButton.setOnClickListener { saveNotes() }
        binding.checkDistanceButton.setOnClickListener { checkCurrentDistance(false) }
        binding.markVisitedButton.setOnClickListener { checkCurrentDistance(true) }

        lifecycleScope.launch {
            repository.seedDatabaseIfNeeded()
            loadPlace()
        }
    }

    /** Loads the latest Room record so notes, photo, and status stay synchronized. */
    private suspend fun loadPlace() {
        val place = repository.getPlaceById(placeId) ?: run {
            finish()
            return
        }
        displayedPlace = place
        val currentPlace = repository.getCurrentPlace()
        val isCurrent = currentPlace?.id == place.id
        val isUnlocked = place.isVisited || isCurrent
        isCurrentStop = isCurrent

        supportActionBar?.title =
            if (isUnlocked) place.name else getString(R.string.mystery_stop)
        binding.stopText.text = getString(R.string.stop_of_total, place.huntOrder, TOTAL_STOPS)
        binding.nameText.text = if (isUnlocked) place.name else getString(R.string.mystery_stop)
        binding.addressText.text =
            if (isUnlocked) place.address else getString(R.string.locked_stop_message)
        binding.clueText.text =
            if (isUnlocked) place.clue else getString(R.string.locked_clue_message)
        binding.notesInput.setText(if (isUnlocked) place.notes else "")
        showPhoto(if (isUnlocked) place.photoUri else null)

        binding.notesInput.isEnabled = isUnlocked
        binding.saveNotesButton.isEnabled = isUnlocked
        binding.choosePhotoButton.isEnabled = isUnlocked
        binding.openMapButton.isEnabled = isUnlocked
        binding.checkDistanceButton.isEnabled = isCurrent

        when {
            place.isVisited -> {
                binding.statusText.setText(R.string.visited)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.aqua))
                binding.markVisitedButton.setText(R.string.already_visited)
                binding.markVisitedButton.isEnabled = false
                binding.distanceStatusText.setText(R.string.visit_already_verified)
            }

            isCurrent -> {
                binding.statusText.setText(R.string.current_stop)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.coral))
                binding.markVisitedButton.setText(R.string.verify_visit)
                binding.markVisitedButton.isEnabled = true
                binding.distanceStatusText.setText(R.string.distance_ready)
            }

            else -> {
                binding.statusText.setText(R.string.locked_status)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                binding.markVisitedButton.setText(R.string.locked)
                binding.markVisitedButton.isEnabled = false
                binding.distanceStatusText.setText(R.string.distance_locked)
            }
        }
    }

    private fun saveNotes() {
        val notes = binding.notesInput.text?.toString()?.trim().orEmpty()
        lifecycleScope.launch {
            repository.updateNotes(placeId, notes)
            Toast.makeText(this@PlaceDetailActivity, R.string.notes_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhoto(photoUri: String?) {
        if (photoUri.isNullOrBlank()) {
            binding.photoPreview.setImageResource(R.drawable.ic_treasure_photo)
            return
        }

        try {
            binding.photoPreview.setImageURI(Uri.parse(photoUri))
        } catch (_: SecurityException) {
            binding.photoPreview.setImageResource(R.drawable.ic_treasure_photo)
        }
    }

    /** Presents camera, gallery, and removal actions in one simple photo menu. */
    private fun showPhotoOptions() {
        val hasPhoto = !displayedPlace?.photoUri.isNullOrBlank()
        val labels = mutableListOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_device)
        )
        if (hasPhoto) labels += getString(R.string.remove_photo)

        AlertDialog.Builder(this)
            .setTitle(R.string.place_photo)
            .setItems(labels.toTypedArray()) { _, index ->
                when (index) {
                    0 -> takePhoto()
                    1 -> photoPicker.launch(arrayOf("image/*"))
                    2 -> removePhoto()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun takePhoto() {
        try {
            val directory = File(filesDir, PHOTO_DIRECTORY).apply { mkdirs() }
            val photoFile = File.createTempFile("stop_${placeId}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            pendingCameraFile = photoFile
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingCameraFile?.delete()
            pendingCameraFile = null
            pendingCameraUri = null
            Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_LONG).show()
        } catch (_: IOException) {
            Toast.makeText(this, R.string.photo_file_error, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun savePhoto(uri: Uri) {
        repository.updatePhoto(placeId, uri.toString())
        displayedPlace = displayedPlace?.copy(photoUri = uri.toString())
        showPhoto(uri.toString())
        Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show()
    }

    private fun removePhoto() {
        lifecycleScope.launch {
            repository.updatePhoto(placeId, null)
            displayedPlace = displayedPlace?.copy(photoUri = null)
            showPhoto(null)
            Toast.makeText(this@PlaceDetailActivity, R.string.photo_removed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Checks the participant's live position and optionally completes a nearby stop. */
    private fun checkCurrentDistance(completeWhenNearby: Boolean) {
        val place = displayedPlace ?: return
        if (place.isVisited || !isCurrentStop) return

        if (!hasLocationPermission()) {
            pendingLocationAction = completeWhenNearby
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        binding.checkDistanceButton.isEnabled = false
        binding.markVisitedButton.isEnabled = false
        binding.distanceStatusText.setText(R.string.checking_location_detail)

        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        evaluateLocation(place, location, completeWhenNearby)
                    } else {
                        locationClient.lastLocation
                            .addOnSuccessListener(this) { lastLocation ->
                                if (lastLocation == null) showLocationUnavailable()
                                else evaluateLocation(place, lastLocation, completeWhenNearby)
                            }
                            .addOnFailureListener(this) { showLocationUnavailable() }
                    }
                }
                .addOnFailureListener(this) { showLocationUnavailable() }
        } catch (_: SecurityException) {
            showLocationUnavailable()
        }
    }

    private fun evaluateLocation(
        place: TreasurePlace,
        location: Location,
        completeWhenNearby: Boolean
    ) {
        val distance = LocationTools.distanceMeters(
            location.latitude,
            location.longitude,
            place.latitude,
            place.longitude
        )
        val distanceLabel = formatDistance(distance)
        restoreLocationButtons()

        if (LocationTools.isWithinVisitRadius(distance)) {
            binding.distanceStatusText.text = getString(R.string.distance_arrived, distanceLabel)
            if (completeWhenNearby) confirmVisit(place, distanceLabel)
        } else {
            binding.distanceStatusText.text = getString(R.string.distance_away, distanceLabel)
            if (completeWhenNearby) {
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
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun showLocationUnavailable() {
        restoreLocationButtons()
        binding.distanceStatusText.setText(R.string.location_unavailable)
        Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun restoreLocationButtons() {
        val place = displayedPlace ?: return
        if (!place.isVisited && isCurrentStop) {
            binding.checkDistanceButton.isEnabled = true
            binding.markVisitedButton.isEnabled = true
            binding.markVisitedButton.setText(R.string.verify_visit)
        }
    }

    private fun confirmVisit(place: TreasurePlace, distanceLabel: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_visit_title)
            .setMessage(getString(R.string.confirm_verified_visit_message, place.name, distanceLabel))
            .setPositiveButton(R.string.mark_visited) { _, _ ->
                lifecycleScope.launch {
                    repository.markVisited(place.id)
                    Toast.makeText(
                        this@PlaceDetailActivity,
                        getString(R.string.stop_completed, place.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatDistance(distanceMeters: Double): String =
        if (distanceMeters < 1_000) {
            getString(R.string.distance_metres, distanceMeters.roundToInt())
        } else {
            getString(R.string.distance_kilometres, distanceMeters / 1_000.0)
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CAMERA_URI, pendingCameraUri?.toString())
        outState.putString(STATE_CAMERA_FILE, pendingCameraFile?.absolutePath)
    }

    companion object {
        const val EXTRA_PLACE_ID = "extra_place_id"
        private const val INVALID_ID = -1
        private const val TOTAL_STOPS = 20
        private const val PHOTO_DIRECTORY = "treasure_photos"
        private const val STATE_CAMERA_URI = "state_camera_uri"
        private const val STATE_CAMERA_FILE = "state_camera_file"
    }
}
