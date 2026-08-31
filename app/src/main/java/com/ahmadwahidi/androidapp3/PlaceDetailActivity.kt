package com.ahmadwahidi.androidapp3

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ahmadwahidi.androidapp3.data.AppDatabase
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.ahmadwahidi.androidapp3.data.TreasureRepository
import com.ahmadwahidi.androidapp3.databinding.ActivityPlaceDetailBinding
import kotlinx.coroutines.launch

/** Detail screen with clue, map action, personal notes, and a saved place photo. */
class PlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceDetailBinding
    private lateinit var repository: TreasureRepository
    private var placeId: Int = INVALID_ID
    private var displayedPlace: TreasurePlace? = null

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
                repository.updatePhoto(placeId, uri.toString())
                showPhoto(uri.toString())
                Toast.makeText(
                    this@PlaceDetailActivity,
                    R.string.photo_saved,
                    Toast.LENGTH_SHORT
                ).show()
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

        placeId = intent.getIntExtra(EXTRA_PLACE_ID, INVALID_ID)
        if (placeId == INVALID_ID) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        binding.openMapButton.setOnClickListener { displayedPlace?.let(::openInMaps) }
        binding.choosePhotoButton.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }
        binding.saveNotesButton.setOnClickListener { saveNotes() }
        binding.markVisitedButton.setOnClickListener { confirmVisit() }

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

        supportActionBar?.title = place.name
        binding.stopText.text = getString(R.string.stop_of_total, place.huntOrder, TOTAL_STOPS)
        binding.nameText.text = if (isUnlocked) place.name else getString(R.string.mystery_stop)
        binding.addressText.text =
            if (isUnlocked) place.address else getString(R.string.locked_stop_message)
        binding.clueText.text =
            if (isUnlocked) place.clue else getString(R.string.locked_clue_message)
        binding.notesInput.setText(place.notes)
        showPhoto(place.photoUri)

        binding.notesInput.isEnabled = isUnlocked
        binding.saveNotesButton.isEnabled = isUnlocked
        binding.choosePhotoButton.isEnabled = isUnlocked
        binding.openMapButton.isEnabled = isUnlocked

        when {
            place.isVisited -> {
                binding.statusText.setText(R.string.visited)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.aqua))
                binding.markVisitedButton.setText(R.string.already_visited)
                binding.markVisitedButton.isEnabled = false
            }

            isCurrent -> {
                binding.statusText.setText(R.string.current_stop)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.coral))
                binding.markVisitedButton.setText(R.string.mark_as_visited)
                binding.markVisitedButton.isEnabled = true
            }

            else -> {
                binding.statusText.setText(R.string.locked_status)
                binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                binding.markVisitedButton.setText(R.string.locked)
                binding.markVisitedButton.isEnabled = false
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

    private fun confirmVisit() {
        val place = displayedPlace ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_visit_title)
            .setMessage(getString(R.string.confirm_visit_message, place.name))
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

    companion object {
        const val EXTRA_PLACE_ID = "extra_place_id"
        private const val INVALID_ID = -1
        private const val TOTAL_STOPS = 20
    }
}
