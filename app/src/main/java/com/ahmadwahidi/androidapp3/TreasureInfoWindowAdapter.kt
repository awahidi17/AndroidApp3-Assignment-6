package com.ahmadwahidi.androidapp3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.ahmadwahidi.androidapp3.data.TreasurePlace
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

/** Builds the colourful information card shown when a treasure marker is tapped. */
class TreasureInfoWindowAdapter(context: Context) : GoogleMap.InfoWindowAdapter {

    private val cardView = LayoutInflater.from(context).inflate(R.layout.map_info_window, null)
    private val stopText = cardView.findViewById<TextView>(R.id.infoStopText)
    private val nameText = cardView.findViewById<TextView>(R.id.infoNameText)
    private val addressText = cardView.findViewById<TextView>(R.id.infoAddressText)
    private val actionText = cardView.findViewById<TextView>(R.id.infoActionText)

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View {
        val place = marker.tag as? TreasurePlace
        if (place != null) {
            stopText.text = cardView.context.getString(R.string.info_stop, place.huntOrder)
            nameText.text = place.name
            addressText.text = place.address
            actionText.setText(
                if (place.isVisited) R.string.info_visited_action
                else R.string.info_current_action
            )
        }
        return cardView
    }
}
