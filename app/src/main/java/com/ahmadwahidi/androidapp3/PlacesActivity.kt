package com.ahmadwahidi.androidapp3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ahmadwahidi.androidapp3.data.AppDatabase
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.ahmadwahidi.androidapp3.data.TreasureRepository
import com.ahmadwahidi.androidapp3.databinding.ActivityPlacesBinding
import kotlinx.coroutines.launch

/** RecyclerView screen showing visited, current, and locked hunt stops. */
class PlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlacesBinding
    private lateinit var repository: TreasureRepository
    private lateinit var adapter: TreasurePlaceAdapter
    private var allPlaces: List<TreasurePlace> = emptyList()
    private var currentPlaceId: Int? = null

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
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderSearch(query.orEmpty())
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                renderSearch(newText.orEmpty())
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            repository.seedDatabaseIfNeeded()
            allPlaces = repository.getAllPlaces()
            currentPlaceId = repository.getCurrentPlace()?.id
            val visited = repository.getVisitedCount()
            binding.summaryText.text = getString(R.string.stops_summary, visited, allPlaces.size)
            renderSearch(binding.searchView.query?.toString().orEmpty())
        }
    }

    /** Filters only revealed stops so a search can never expose a future business. */
    private fun renderSearch(query: String) {
        val searchTerm = query.trim()
        val filteredPlaces = if (searchTerm.isBlank()) {
            allPlaces
        } else {
            allPlaces.filter { place ->
                val isUnlocked = place.isVisited || place.id == currentPlaceId
                isUnlocked && (
                    place.name.contains(searchTerm, ignoreCase = true) ||
                        place.address.contains(searchTerm, ignoreCase = true) ||
                        place.huntOrder.toString() == searchTerm.removePrefix("#")
                    )
            }
        }

        adapter.submitPlaces(filteredPlaces, currentPlaceId)
        binding.emptySearchText.isVisible = filteredPlaces.isEmpty()
        binding.placesRecyclerView.isVisible = filteredPlaces.isNotEmpty()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
