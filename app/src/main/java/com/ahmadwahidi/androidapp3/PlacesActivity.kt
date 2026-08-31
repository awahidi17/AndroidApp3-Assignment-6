package com.ahmadwahidi.androidapp3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ahmadwahidi.androidapp3.data.AppDatabase
import com.ahmadwahidi.androidapp3.data.TreasureRepository
import com.ahmadwahidi.androidapp3.databinding.ActivityPlacesBinding
import kotlinx.coroutines.launch

/** RecyclerView screen showing visited, current, and locked hunt stops. */
class PlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlacesBinding
    private lateinit var repository: TreasureRepository
    private lateinit var adapter: TreasurePlaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.all_hunt_stops)

        repository = TreasureRepository(
            AppDatabase.getDatabase(applicationContext).treasurePlaceDao()
        )

        adapter = TreasurePlaceAdapter { place ->
            startActivity(
                Intent(this, PlaceDetailActivity::class.java)
                    .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.id)
            )
        }

        binding.placesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.placesRecyclerView.adapter = adapter
        binding.backButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            repository.seedDatabaseIfNeeded()
            val places = repository.getAllPlaces()
            val currentId = repository.getCurrentPlace()?.id
            val visited = repository.getVisitedCount()
            binding.summaryText.text = getString(R.string.stops_summary, visited, places.size)
            adapter.submitPlaces(places, currentId)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
