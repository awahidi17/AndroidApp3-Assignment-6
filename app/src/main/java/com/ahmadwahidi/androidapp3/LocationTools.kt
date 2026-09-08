package com.ahmadwahidi.androidapp3

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure location calculations shared by both visit-verification screens. */
object LocationTools {

    // A 250 m radius tolerates normal phone GPS drift while still requiring an in-person visit.
    const val VISIT_RADIUS_METERS = 250.0

    /** Returns the straight-line distance between two coordinates using the Haversine formula. */
    fun distanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val latitudeChange = Math.toRadians(endLatitude - startLatitude)
        val longitudeChange = Math.toRadians(endLongitude - startLongitude)
        val startLatitudeRadians = Math.toRadians(startLatitude)
        val endLatitudeRadians = Math.toRadians(endLatitude)

        val haversine = sin(latitudeChange / 2) * sin(latitudeChange / 2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeChange / 2) * sin(longitudeChange / 2)
        val safeHaversine = haversine.coerceIn(0.0, 1.0)
        val angle = 2 * atan2(sqrt(safeHaversine), sqrt(1 - safeHaversine))
        return EARTH_RADIUS_METERS * angle
    }

    fun isWithinVisitRadius(distanceMeters: Double): Boolean =
        distanceMeters <= VISIT_RADIUS_METERS

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
