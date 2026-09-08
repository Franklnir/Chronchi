package com.irsyadlabs.espbridge.data.firmware

import com.irsyadlabs.espbridge.core.model.AndroidFirmwareUpdatePolicy
import com.irsyadlabs.espbridge.transport.ble.ConnectedDeviceInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class FirmwareUpdateRepositoryTest {
    @Test
    fun `C3 check is rejected before accessing release channel`() = runBlocking {
        val repository = FirmwareUpdateRepository(
            manifestUrl = "https://127.0.0.1/this-must-not-be-opened.json",
            publicKeyBase64 = "configured-but-unused"
        )
        val device = ConnectedDeviceInfo(
            id = "chronchi-test",
            name = "Chronchi",
            firmwareVersion = "2.1.0",
            board = "esp32c3-inmp441",
            otaAvailable = true,
            maximumImageSize = 2_752_512,
            secureLinkRequired = true,
            runningSlot = "ota_0",
            updateStrategy = "recovery",
            role = "main"
        )

        val result = repository.check(device)

        assertTrue(result is FirmwareCheckResult.Unavailable)
        assertEquals(
            AndroidFirmwareUpdatePolicy.C3_BLOCK_REASON,
            (result as FirmwareCheckResult.Unavailable).reason
        )
    }

    @Test
    fun `semantic version comparison handles different segment counts`() {
        assertTrue(FirmwareUpdateRepository.compareVersions("2.2.0", "2.1.9") > 0)
        assertEquals(0, FirmwareUpdateRepository.compareVersions("2.1", "2.1.0"))
        assertTrue(FirmwareUpdateRepository.compareVersions("2.1.0", "3.0.0") < 0)
    }

    @Test
    fun `ECDSA manifest signature verifies and rejects mutations`() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keys = generator.generateKeyPair()
        val unsigned = FirmwareManifest(
            schema = 2,
            board = "esp32c3-inmp441-production",
            version = "2.2.0",
            size = 2_640_000,
            sha256 = "ab".repeat(32),
            url = "https://updates.example.com/chronchi/2.2.0/xiaozhi.bin",
            mandatory = false,
            notes = "Production stability update",
            deviceSignature = Base64.getEncoder().encodeToString(ByteArray(64) { 0x42 }),
            signature = ""
        )
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keys.private)
        signer.update(unsigned.canonicalPayload())
        val manifest = unsigned.copy(
            signature = Base64.getEncoder().encodeToString(signer.sign())
        )
        val repository = FirmwareUpdateRepository(
            manifestUrl = "https://updates.example.com/chronchi/manifest.json",
            publicKeyBase64 = Base64.getEncoder().encodeToString(keys.public.encoded)
        )

        assertTrue(repository.verifyManifest(manifest))
        assertFalse(repository.verifyManifest(manifest.copy(size = manifest.size + 1)))
    }
}
