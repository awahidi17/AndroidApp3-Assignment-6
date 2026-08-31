package com.ahmadwahidi.androidapp3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One stop in the Windsor treasure hunt. Room saves progress, notes, and a photo URI. */
@Entity(tableName = "treasure_places")
data class TreasurePlace(
    @PrimaryKey val id: Int,
    val huntOrder: Int,
    val name: String,
    val address: String,
    val clue: String,
    val latitude: Double,
    val longitude: Double,
    val isVisited: Boolean = false,
    val notes: String = "",
    val photoUri: String? = null
)
