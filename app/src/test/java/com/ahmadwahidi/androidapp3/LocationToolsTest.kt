package com.ahmadwahidi.androidapp3

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the proximity rule used to complete a treasure stop. */
class LocationToolsTest {

    @Test
    fun sameCoordinateIsInsideVisitRadius() {
        val distance = LocationTools.distanceMeters(
            42.31721,
            -83.03541,
            42.31721,
            -83.03541
        )

        assertTrue(LocationTools.isWithinVisitRadius(distance))
    }

    @Test
    fun distantCoordinateIsOutsideVisitRadius() {
        val distance = LocationTools.distanceMeters(
            42.31721,
            -83.03541,
            42.32209,
            -83.01147
        )

        assertFalse(LocationTools.isWithinVisitRadius(distance))
    }
}
