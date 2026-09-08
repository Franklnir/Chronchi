package com.irsyadlabs.espbridge.core.security

import com.irsyadlabs.espbridge.data.local.SettingsRepository
import kotlinx.coroutines.flow.first
import java.security.SecureRandom

class DeviceCredentialsManager(
    private val settingsRepository: SettingsRepository,
    private val secretStore: KeystoreSecretStore
) {
    data class Credentials(val deviceId: String, val accessKey: String, val secretKey: String)

    suspend fun getOrCreate(): Credentials {
        val settings = settingsRepository.settings.first()
        val deviceId = settings.deviceId ?: "ESP-${randomToken(4).uppercase()}".also {
            settingsRepository.setDeviceId(it)
        }
        val accessKey = secretStore.get("access_key") ?: "AK_${randomToken(12)}".also {
            secretStore.put("access_key", it)
        }
        val secretKey = secretStore.get("secret_key") ?: "SK_${randomToken(24)}".also {
            secretStore.put("secret_key", it)
        }
        return Credentials(deviceId, accessKey, secretKey)
    }

    suspend fun regenerate(): Credentials {
        val settings = settingsRepository.settings.first()
        val deviceId = settings.deviceId ?: "ESP-${randomToken(4).uppercase()}".also {
            settingsRepository.setDeviceId(it)
        }
        val accessKey = "AK_${randomToken(12)}"
        val secretKey = "SK_${randomToken(24)}"
        secretStore.put("access_key", accessKey)
        secretStore.put("secret_key", secretKey)
        return Credentials(deviceId, accessKey, secretKey)
    }

    private fun randomToken(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }
}
