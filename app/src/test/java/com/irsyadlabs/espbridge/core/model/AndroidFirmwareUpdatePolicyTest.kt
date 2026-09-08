package com.irsyadlabs.espbridge.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidFirmwareUpdatePolicyTest {
    @Test
    fun `ESP32 C3 is rejected even when device advertises OTA`() {
        val access = AndroidFirmwareUpdatePolicy.evaluate(
            board = "esp32c3-inmp441",
            otaAdvertised = true
        )

        assertFalse(access.allowed)
        assertEquals(AndroidFirmwareUpdatePolicy.C3_BLOCK_REASON, access.reason)
    }

    @Test
    fun `unreleased future board is not implicitly enabled`() {
        val access = AndroidFirmwareUpdatePolicy.evaluate(
            board = "future-board",
            otaAdvertised = true
        )

        assertFalse(access.allowed)
        assertEquals(AndroidFirmwareUpdatePolicy.NOT_RELEASED_REASON, access.reason)
    }

    @Test
    fun `device capability cannot override product policy`() {
        val advertised = AndroidFirmwareUpdatePolicy.evaluate("esp32-c3-mini", true)
        val notAdvertised = AndroidFirmwareUpdatePolicy.evaluate("esp32-c3-mini", false)

        assertFalse(advertised.allowed)
        assertFalse(notAdvertised.allowed)
    }
}
