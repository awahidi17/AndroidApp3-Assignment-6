package com.ahmadwahidi.androidapp3

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.ahmadwahidi.androidapp3.databinding.ItemTreasurePlaceBinding

/** Binds the 20 Room records to the All Stops RecyclerView. */
class TreasurePlaceAdapter(
    private val onPlaceClicked: (TreasurePlace) -> Unit
) : RecyclerView.Adapter<TreasurePlaceAdapter.PlaceViewHolder>() {

    private val places = mutableListOf<TreasurePlace>()
    private var currentPlaceId: Int? = null

    fun submitPlaces(newPlaces: List<TreasurePlace>, currentId: Int?) {
        places.clear()
        places.addAll(newPlaces)
        currentPlaceId = currentId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemTreasurePlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size

    inner class PlaceViewHolder(
        private val binding: ItemTreasurePlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: TreasurePlace) {
            val context = binding.root.context
            val isCurrent = place.id == currentPlaceId
            val isUnlocked = place.isVisited || isCurrent

            binding.stopNumberText.text = context.getString(R.string.stop_number_short, place.huntOrder)
            binding.placeNameText.text = if (isUnlocked) place.name else context.getString(R.string.mystery_stop)
            binding.addressText.text =
                if (isUnlocked) place.address else context.getString(R.string.locked_stop_message)

            val status = when {
                place.isVisited -> Triple(R.string.visited, R.color.aqua, R.color.aqua_soft)
                isCurrent -> Triple(R.string.current, R.color.coral, R.color.coral_soft)
                else -> Triple(R.string.locked, R.color.text_muted, R.color.locked_soft)
            }

            binding.statusText.setText(status.first)
            binding.statusText.setTextColor(ContextCompat.getColor(context, status.second))
            binding.statusText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, status.third))
            binding.stopNumberText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, status.second))
            binding.root.strokeColor = ContextCompat.getColor(context, status.second)
            binding.root.alpha = if (isUnlocked) 1f else 0.7f
            binding.root.isClickable = isUnlocked
            binding.root.isFocusable = isUnlocked
            binding.root.setOnClickListener {
                if (isUnlocked) onPlaceClicked(place)
            }
        }
    }
}
