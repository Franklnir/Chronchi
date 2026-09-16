package com.irsyadlabs.espbridge.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.core.model.ConnectionMode
import com.irsyadlabs.espbridge.core.model.SupportedSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.espBridgeDataStore by preferencesDataStore(name = "espbridge_settings")

data class LocalSettings(
    val onboardingComplete: Boolean = false,
    val selectedSourceIds: Set<String> = SupportedSources.all.filter { it.defaultEnabled }.map { it.id }.toSet(),
    val connectionMode: ConnectionMode = ConnectionMode.AUTOMATIC,
    val autoConnect: Boolean = true,
    val cloudSyncEnabled: Boolean = true,
    val trustedDeviceAddress: String? = null,
    val trustedDeviceName: String? = null,
    val deviceId: String? = null,
    val demoLoggedIn: Boolean = false,
    val demoEmail: String? = null,
    val weatherTemperature: Double? = null,
    val weatherCondition: String? = null,
    val weatherLocation: String? = null,
    val weatherUpdatedAt: Long = 0L,
    val keepBackgroundConnection: Boolean = true,
    val appTheme: AppTheme = AppTheme.ILLUSTRATIVE,
    val firebaseDatabaseSecret: String? = null,
    // Dual Operating Mode & Xiaozhi AI Session
    val operatingMode: String = "", // "CHRONCHI_BLE" or "XIAOZHI_AI"
    val xiaozhiAccessToken: String? = null,
    val xiaozhiRefreshToken: String? = null,
    val xiaozhiUsername: String? = null,
    val xiaozhiUserId: Int? = null,
    val xiaozhiRole: String? = null,
    val xiaozhiMcpConnected: Boolean = false,
    val xiaozhiMcpTokenPreview: String? = null
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val selectedSources = stringSetPreferencesKey("selected_sources")
        val connectionMode = stringPreferencesKey("connection_mode")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val cloudSync = booleanPreferencesKey("cloud_sync")
        val trustedDeviceAddress = stringPreferencesKey("trusted_device_address")
        val trustedDeviceName = stringPreferencesKey("trusted_device_name")
        val deviceId = stringPreferencesKey("device_id")
        val demoLoggedIn = booleanPreferencesKey("demo_logged_in")
        val demoEmail = stringPreferencesKey("demo_email")
        val weatherTemperatureMilli = intPreferencesKey("weather_temperature_milli")
        val weatherCondition = stringPreferencesKey("weather_condition")
        val weatherLocation = stringPreferencesKey("weather_location")
        val weatherUpdatedAt = longPreferencesKey("weather_updated_at")
        val keepBackgroundConnection = booleanPreferencesKey("keep_background_connection")
        val appTheme = stringPreferencesKey("app_theme")
        val firebaseDatabaseSecret = stringPreferencesKey("firebase_database_secret")
        // Xiaozhi Keys
        val operatingMode = stringPreferencesKey("operating_mode")
        val xiaozhiAccessToken = stringPreferencesKey("xiaozhi_access_token")
        val xiaozhiRefreshToken = stringPreferencesKey("xiaozhi_refresh_token")
        val xiaozhiUsername = stringPreferencesKey("xiaozhi_username")
        val xiaozhiUserId = intPreferencesKey("xiaozhi_user_id")
        val xiaozhiRole = stringPreferencesKey("xiaozhi_role")
        val xiaozhiMcpConnected = booleanPreferencesKey("xiaozhi_mcp_connected")
        val xiaozhiMcpTokenPreview = stringPreferencesKey("xiaozhi_mcp_token_preview")
    }

    val settings: Flow<LocalSettings> = context.espBridgeDataStore.data.map { prefs ->
        val defaultSources = SupportedSources.all.filter { it.defaultEnabled }.map { it.id }.toSet()
        LocalSettings(
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
            selectedSourceIds = prefs[Keys.selectedSources] ?: defaultSources,
            connectionMode = runCatching {
                ConnectionMode.valueOf(prefs[Keys.connectionMode] ?: ConnectionMode.AUTOMATIC.name)
            }.getOrDefault(ConnectionMode.AUTOMATIC),
            autoConnect = prefs[Keys.autoConnect] ?: true,
            cloudSyncEnabled = prefs[Keys.cloudSync] ?: true,
            trustedDeviceAddress = prefs[Keys.trustedDeviceAddress],
            trustedDeviceName = prefs[Keys.trustedDeviceName],
            deviceId = prefs[Keys.deviceId],
            demoLoggedIn = prefs[Keys.demoLoggedIn] ?: false,
            demoEmail = prefs[Keys.demoEmail],
            weatherTemperature = prefs[Keys.weatherTemperatureMilli]?.div(1000.0),
            weatherCondition = prefs[Keys.weatherCondition],
            weatherLocation = prefs[Keys.weatherLocation],
            weatherUpdatedAt = prefs[Keys.weatherUpdatedAt] ?: 0L,
            keepBackgroundConnection = prefs[Keys.keepBackgroundConnection] ?: true,
            appTheme = runCatching {
                AppTheme.valueOf(prefs[Keys.appTheme] ?: AppTheme.ILLUSTRATIVE.name)
            }.getOrDefault(AppTheme.ILLUSTRATIVE),
            firebaseDatabaseSecret = prefs[Keys.firebaseDatabaseSecret],
            operatingMode = prefs[Keys.operatingMode] ?: "",
            xiaozhiAccessToken = prefs[Keys.xiaozhiAccessToken],
            xiaozhiRefreshToken = prefs[Keys.xiaozhiRefreshToken],
            xiaozhiUsername = prefs[Keys.xiaozhiUsername],
            xiaozhiUserId = prefs[Keys.xiaozhiUserId],
            xiaozhiRole = prefs[Keys.xiaozhiRole],
            xiaozhiMcpConnected = prefs[Keys.xiaozhiMcpConnected] ?: false,
            xiaozhiMcpTokenPreview = prefs[Keys.xiaozhiMcpTokenPreview]
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) = context.espBridgeDataStore.edit {
        it[Keys.onboardingComplete] = value
    }

    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean) = context.espBridgeDataStore.edit { prefs ->
        val current = (prefs[Keys.selectedSources]
            ?: SupportedSources.all.filter { it.defaultEnabled }.map { it.id }.toSet()).toMutableSet()
        if (enabled) current += sourceId else current -= sourceId
        prefs[Keys.selectedSources] = current
    }

    suspend fun setConnectionMode(mode: ConnectionMode) = context.espBridgeDataStore.edit {
        it[Keys.connectionMode] = mode.name
    }

    suspend fun setAutoConnect(enabled: Boolean) = context.espBridgeDataStore.edit {
        it[Keys.autoConnect] = enabled
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) = context.espBridgeDataStore.edit {
        it[Keys.cloudSync] = enabled
    }

    suspend fun setKeepBackgroundConnection(enabled: Boolean) = context.espBridgeDataStore.edit {
        it[Keys.keepBackgroundConnection] = enabled
    }

    suspend fun setAppTheme(theme: AppTheme) = context.espBridgeDataStore.edit {
        it[Keys.appTheme] = theme.name
    }

    suspend fun setTrustedDevice(address: String?, name: String?) = context.espBridgeDataStore.edit {
        if (address == null) {
            it.remove(Keys.trustedDeviceAddress)
            it.remove(Keys.trustedDeviceName)
        } else {
            it[Keys.trustedDeviceAddress] = address
            if (name != null) it[Keys.trustedDeviceName] = name else it.remove(Keys.trustedDeviceName)
        }
    }

    suspend fun setDeviceId(deviceId: String) = context.espBridgeDataStore.edit {
        it[Keys.deviceId] = deviceId
    }

    suspend fun setDemoSession(email: String?) = context.espBridgeDataStore.edit {
        it[Keys.demoLoggedIn] = email != null
        if (email != null) it[Keys.demoEmail] = email else it.remove(Keys.demoEmail)
    }

    suspend fun cacheWeather(temperatureC: Double, condition: String, location: String, updatedAt: Long) =
        context.espBridgeDataStore.edit {
            it[Keys.weatherTemperatureMilli] = (temperatureC * 1000).toInt()
            it[Keys.weatherCondition] = condition
            it[Keys.weatherLocation] = location
            it[Keys.weatherUpdatedAt] = updatedAt
        }

    suspend fun clearLocalSession() = context.espBridgeDataStore.edit {
        it.remove(Keys.demoLoggedIn)
        it.remove(Keys.demoEmail)
    }

    suspend fun setFirebaseDatabaseSecret(secret: String?) = context.espBridgeDataStore.edit {
        if (secret.isNullOrEmpty()) {
            it.remove(Keys.firebaseDatabaseSecret)
        } else {
            it[Keys.firebaseDatabaseSecret] = secret
        }
    }

    // Xiaozhi session methods
    suspend fun setOperatingMode(mode: String) = context.espBridgeDataStore.edit {
        it[Keys.operatingMode] = mode
    }

    suspend fun saveXiaozhiSession(
        token: String,
        refreshToken: String?,
        username: String,
        userId: Int?,
        role: String?,
        mcpConnected: Boolean,
        preview: String? = null
    ) = context.espBridgeDataStore.edit {
        it[Keys.xiaozhiAccessToken] = token
        if (refreshToken != null) it[Keys.xiaozhiRefreshToken] = refreshToken else it.remove(Keys.xiaozhiRefreshToken)
        it[Keys.xiaozhiUsername] = username
        if (userId != null) it[Keys.xiaozhiUserId] = userId else it.remove(Keys.xiaozhiUserId)
        if (role != null) it[Keys.xiaozhiRole] = role else it.remove(Keys.xiaozhiRole)
        it[Keys.xiaozhiMcpConnected] = mcpConnected
        if (preview != null) it[Keys.xiaozhiMcpTokenPreview] = preview else it.remove(Keys.xiaozhiMcpTokenPreview)
    }

    suspend fun setXiaozhiMcpConnected(connected: Boolean, preview: String? = null) = context.espBridgeDataStore.edit {
        it[Keys.xiaozhiMcpConnected] = connected
        if (preview != null) it[Keys.xiaozhiMcpTokenPreview] = preview
    }

    suspend fun clearXiaozhiSession() = context.espBridgeDataStore.edit {
        it.remove(Keys.xiaozhiAccessToken)
        it.remove(Keys.xiaozhiRefreshToken)
        it.remove(Keys.xiaozhiUsername)
        it.remove(Keys.xiaozhiUserId)
        it.remove(Keys.xiaozhiRole)
        it[Keys.xiaozhiMcpConnected] = false
        it.remove(Keys.xiaozhiMcpTokenPreview)
    }
}
