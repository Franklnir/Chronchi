package com.irsyadlabs.espbridge.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OledPreviewSelectorTest {
    @Test
    fun `recent message selects message template`() {
        val event = DisplayEvent(
            category = DisplayCategory.MESSAGE,
            sourceApp = "WhatsApp",
            primaryText = "Budi",
            secondaryText = "Halo",
            timestamp = 1_000L
        )
        val result = OledPreviewSelector.select(
            PhoneState(liveData = LiveDataState(event = event)),
            nowMillis = 5_999L
        )

        assertEquals(OledTemplate.MESSAGE, result.template)
        assertEquals(event, result.event)
    }

    @Test
    fun `expired notification returns to home`() {
        val event = DisplayEvent(
            category = DisplayCategory.ORDER,
            sourceApp = "Shopee",
            primaryText = "Sedang Diantar",
            timestamp = 1_000L
        )
        val result = OledPreviewSelector.select(
            PhoneState(liveData = LiveDataState(event = event)),
            nowMillis = 6_001L
        )

        assertEquals(OledTemplate.HOME, result.template)
        assertNull(result.event)
    }

    @Test
    fun `active navigation stays above payment`() {
        val payment = DisplayEvent(
            category = DisplayCategory.PAYMENT,
            sourceApp = "OVO",
            primaryText = "+ Rp75.000",
            timestamp = 10_000L
        )
        val navigation = NavigationDisplayState(
            active = true,
            maneuver = Maneuver.RIGHT,
            distanceText = "200 M",
            roadName = "Jl. Ahmad",
            updatedAt = 10_000L
        )
        val result = OledPreviewSelector.select(
            PhoneState(liveData = LiveDataState(event = payment), navigation = navigation),
            nowMillis = 10_100L
        )

        assertEquals(OledTemplate.NAVIGATION, result.template)
        assertEquals(navigation, result.navigation)
        assertEquals(100, OledPreviewSelector.priorityFor(result.template))
    }

    @Test
    fun `payment uses six second timeout`() {
        assertEquals(6_000L, OledPreviewSelector.timeoutFor(DisplayCategory.PAYMENT))
    }
}
