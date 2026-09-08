package com.irsyadlabs.espbridge.collector

import com.irsyadlabs.espbridge.core.model.Maneuver
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationParserTest {
    @Test
    fun parsesIndonesianRightTurn() {
        val result = NavigationParser.parse("Google Maps", "Dalam 200 m, belok kanan ke Jl. Ahmad Yani")
        assertEquals(Maneuver.RIGHT, result.maneuver)
        assertEquals(200, result.distanceMeters)
        assertEquals("Jl. Ahmad Yani", result.roadName)
    }

    @Test
    fun parsesUturn() {
        val result = NavigationParser.parse("Navigasi", "Putar balik dalam 1,2 km")
        assertEquals(Maneuver.UNKNOWN, result.maneuver)
        assertEquals(1200, result.distanceMeters)
    }

    @Test
    fun recognizesNavigationFromExpandedNotificationLines() {
        val result = NavigationParser.parse(
            title = "Google Maps",
            text = "Tetap di rute",
            additionalText = listOf("Dalam 350 m belok kiri menuju Jl. Sudirman")
        )

        assertEquals(Maneuver.LEFT, result.maneuver)
        assertEquals(350, result.distanceMeters)
        assertEquals("Jl. Sudirman", result.roadName)
    }

    @Test
    fun recognizesNavigationSignalBeforeParsing() {
        assertEquals(
            true,
            NavigationParser.looksLikeNavigation("Navigasi", "Dalam 2 km ambil kanan")
        )
    }

    @Test
    fun parsesRoundaboutAndDestinationDistance() {
        val result = NavigationParser.parse(
            "Navigasi",
            "Dalam 200 m masuk bundaran menuju Jl. Ahmad, tujuan 100 km"
        )

        assertEquals(Maneuver.ROUNDABOUT, result.maneuver)
        assertEquals("Tujuan 100 km", result.destinationDistanceText)
    }
}
