package com.ahmadwahidi.androidapp3

import com.ahmadwahidi.androidapp3.data.TreasureSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quick checks for the fixed route used by the treasure hunt. */
class TreasureSeedDataTest {

    @Test
    fun routeContainsTwentySequentialStops() {
        val places = TreasureSeedData.places

        assertEquals(20, places.size)
        assertEquals((1..20).toList(), places.map { it.huntOrder })
        assertEquals(20, places.map { it.id }.distinct().size)
    }

    @Test
    fun routeStartsAtWindsorCityHall() {
        val firstPlace = TreasureSeedData.places.first()

        assertEquals("Windsor City Hall", firstPlace.name)
        assertTrue(firstPlace.address.contains("350 City Hall Square"))
    }
}
