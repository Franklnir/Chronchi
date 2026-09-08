package com.irsyadlabs.espbridge.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronchiLiveStatusFormatterTest {
    @Test
    fun `live status uses the same navigation fields sent to Chronchi`() {
        val navigation = NavigationDisplayState(
            active = true,
            maneuver = Maneuver.RIGHT,
            distanceText = "200 M",
            roadName = "Jl. Ahmad Yani",
            destinationDistanceText = "Tujuan 12 km",
            updatedAt = 1_000L
        )

        val result = ChronchiLiveStatusFormatter.format(
            PhoneState(navigation = navigation),
            nowMillis = 1_100L
        )

        assertEquals("CHRONCHI • NAVIGASI", result.title)
        assertEquals("200 M belok kanan", result.primary)
        assertTrue(result.expandedText.contains("Jl. Ahmad Yani"))
        assertTrue(result.expandedText.contains("Tujuan 12 km"))
    }
}
