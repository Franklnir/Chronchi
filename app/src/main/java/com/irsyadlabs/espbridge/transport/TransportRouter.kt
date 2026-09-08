package com.irsyadlabs.espbridge.transport

import com.irsyadlabs.espbridge.core.model.BleDeliveryState
import com.irsyadlabs.espbridge.core.model.ConnectionMode
import com.irsyadlabs.espbridge.core.model.DisplayEvent
import com.irsyadlabs.espbridge.core.model.LocationState
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import com.irsyadlabs.espbridge.core.model.PhoneState
import com.irsyadlabs.espbridge.core.model.PhoneStatusState
import com.irsyadlabs.espbridge.core.model.WeatherState
import com.irsyadlabs.espbridge.data.firebase.FirebaseCloudTransport
import com.irsyadlabs.espbridge.data.local.SettingsRepository
import com.irsyadlabs.espbridge.core.security.DeviceCredentialsManager
import com.irsyadlabs.espbridge.state.PhoneStateHub
import com.irsyadlabs.espbridge.transport.ble.BleConnectionManager
import com.irsyadlabs.espbridge.transport.ble.OutboundBleMessage
import com.irsyadlabs.espbridge.transport.ble.PacketType
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class TransportRouter(
    private val settingsRepository: SettingsRepository,
    private val ble: BleConnectionManager,
    private val cloud: FirebaseCloudTransport,
    private val stateHub: PhoneStateHub,
    private val credentials: DeviceCredentialsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sendNotification(value: DisplayEvent) = route(
        PacketType.NOTIFICATION,
        "notification",
        notificationMap(value),
        cloudPayload = notificationCloudMap(value),
        onBleQueued = { queued ->
            stateHub.updateBleDelivery(
                value.timestamp,
                if (queued) BleDeliveryState.SENDING else BleDeliveryState.PREVIEW_ONLY
            )
        },
        onBleComplete = { success ->
            stateHub.updateBleDelivery(
                value.timestamp,
                if (success) BleDeliveryState.SENT else BleDeliveryState.PREVIEW_ONLY
            )
        }
    )

    fun sendNavigation(value: NavigationDisplayState) = route(
        PacketType.NAVIGATION,
        "navigation",
        navigationMap(value),
        onBleQueued = { queued ->
            stateHub.updateNavigationBleDelivery(
                value.updatedAt,
                if (queued) BleDeliveryState.SENDING else BleDeliveryState.PREVIEW_ONLY
            )
        },
        onBleComplete = { success ->
            stateHub.updateNavigationBleDelivery(
                value.updatedAt,
                if (success) BleDeliveryState.SENT else BleDeliveryState.PREVIEW_ONLY
            )
        }
    )

    fun sendLocation(value: LocationState) = route(
        PacketType.LOCATION,
        "location",
        locationMap(value)
    )

    fun sendWeather(value: WeatherState) = route(
        PacketType.WEATHER,
        "weather",
        weatherMap(value)
    )

    fun sendPhoneStatus(value: PhoneStatusState) {
        route(PacketType.PHONE_STATUS, "phone", phoneMap(value))
        route(PacketType.NETWORK_STATUS, "network", networkMap(value))
    }

    fun sendTimeSync() {
        route(PacketType.TIME_SYNC, "time", timeMap())
    }

    fun fullSync(state: PhoneState) {
        scope.launch {
            val settings = settingsRepository.settings.first()
            if (ble.isConnected() && settings.connectionMode != ConnectionMode.CLOUD_ONLY) {
                val latestEvent = state.liveData.event
                val messages = buildList {
                    add(OutboundBleMessage(PacketType.SYNC_BEGIN, "{}"))
                    add(OutboundBleMessage(PacketType.TIME_SYNC, json(timeMap())))
                    add(OutboundBleMessage(PacketType.PHONE_STATUS, json(phoneMap(state.phoneStatus))))
                    add(OutboundBleMessage(PacketType.NETWORK_STATUS, json(networkMap(state.phoneStatus))))
                    if (state.weather.temperatureC != null) {
                        add(OutboundBleMessage(PacketType.WEATHER, json(weatherMap(state.weather))))
                    }
                    if (state.location.latitude != null) {
                        add(OutboundBleMessage(PacketType.LOCATION, json(locationMap(state.location))))
                    }
                    add(
                        OutboundBleMessage(
                            PacketType.NAVIGATION,
                            json(navigationMap(state.navigation))
                        ) { success ->
                            stateHub.updateNavigationBleDelivery(
                                state.navigation.updatedAt,
                                if (success) BleDeliveryState.SENT else BleDeliveryState.PREVIEW_ONLY
                            )
                        }
                    )
                    if (latestEvent != null) {
                        add(
                            OutboundBleMessage(
                                PacketType.NOTIFICATION,
                                json(notificationMap(latestEvent))
                            ) { success ->
                                stateHub.updateBleDelivery(
                                    latestEvent.timestamp,
                                    if (success) BleDeliveryState.SENT else BleDeliveryState.PREVIEW_ONLY
                                )
                            }
                        )
                    }
                    add(OutboundBleMessage(PacketType.SYNC_END, "{}"))
                }
                val queued = ble.sendBatch(messages)
                stateHub.updateNavigationBleDelivery(
                    state.navigation.updatedAt,
                    if (queued) BleDeliveryState.SENDING else BleDeliveryState.PREVIEW_ONLY
                )
                latestEvent?.let {
                    stateHub.updateBleDelivery(
                        it.timestamp,
                        if (queued) BleDeliveryState.SENDING else BleDeliveryState.PREVIEW_ONLY
                    )
                }
            }
            val deviceId = settings.deviceId ?: return@launch
            if (settings.cloudSyncEnabled && settings.connectionMode != ConnectionMode.BLUETOOTH_ONLY && cloud.available()) {
                val secret = credentials.getOrCreate().secretKey
                runCatching { cloud.pushSnapshot(deviceId, state, secret) }
            }
        }
    }

    private fun route(
        type: PacketType,
        cloudKey: String,
        payload: Map<String, Any?>,
        cloudPayload: Map<String, Any?> = payload,
        onBleQueued: ((Boolean) -> Unit)? = null,
        onBleComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            val settings = settingsRepository.settings.first()
            val json = json(payload)

            val queuedForBle = if (settings.connectionMode != ConnectionMode.CLOUD_ONLY && ble.isConnected()) {
                ble.send(type, json, onBleComplete)
            } else false
            onBleQueued?.invoke(queuedForBle)

            val deviceId = settings.deviceId
            if (
                deviceId != null &&
                settings.cloudSyncEnabled &&
                settings.connectionMode != ConnectionMode.BLUETOOTH_ONLY &&
                cloud.available()
            ) {
                val secret = credentials.getOrCreate().secretKey
                runCatching { cloud.pushLatest(deviceId, cloudKey, cloudPayload.filterValues { it != null }, secret) }
            }
        }
    }

    private fun notificationMap(v: DisplayEvent) = mapOf(
        "sourceApp" to v.sourceApp,
        "category" to v.category.name,
        "primaryText" to v.primaryText,
        "secondaryText" to v.secondaryText,
        "tertiaryText" to v.tertiaryText,
        "timestamp" to v.timestamp,
        "paymentDirection" to v.paymentDirection?.name,
        "orderStatus" to v.orderStatus?.name
    )

    private fun notificationCloudMap(v: DisplayEvent) = mapOf(
        "sourceApp" to v.sourceApp,
        "category" to v.category.name,
        "primaryText" to v.primaryText,
        "secondaryText" to v.secondaryText,
        "tertiaryText" to v.tertiaryText,
        "timestamp" to v.timestamp
    )

    private fun navigationMap(v: NavigationDisplayState) = mapOf(
        "active" to v.active,
        "maneuver" to v.maneuver.name,
        "distanceMeters" to v.distanceMeters,
        "distanceText" to v.distanceText,
        "roadName" to v.roadName,
        "destinationDistanceText" to v.destinationDistanceText,
        "time" to v.time,
        "updatedAt" to v.updatedAt
    )

    private fun locationMap(v: LocationState) = mapOf(
        "latitude" to v.latitude,
        "longitude" to v.longitude,
        "accuracyMeters" to v.accuracyMeters,
        "speedMps" to v.speedMps,
        "bearingDegrees" to v.bearingDegrees,
        "updatedAt" to v.updatedAt
    )

    private fun weatherMap(v: WeatherState) = mapOf(
        "temperatureC" to v.temperatureC,
        "condition" to v.condition,
        "humidity" to v.humidity,
        "location" to v.locationLabel,
        "updatedAt" to v.updatedAt,
        "cached" to v.isCached
    )

    private fun phoneMap(v: PhoneStatusState) = mapOf(
        "batteryLevel" to v.batteryLevel,
        "charging" to v.charging,
        "bluetoothEnabled" to v.bluetoothEnabled,
        "espConnected" to v.espConnected,
        "updatedAt" to v.updatedAt
    )

    private fun networkMap(v: PhoneStatusState) = mapOf(
        "transport" to v.networkTransport,
        "generation" to v.networkGeneration,
        "signalLevel" to v.signalLevel,
        "updatedAt" to v.updatedAt
    )

    private fun timeMap(): Map<String, Any?> {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        return mapOf(
            "epochSeconds" to now.epochSecond,
            "timezone" to zone.id,
            "utcOffsetMinutes" to zone.rules.getOffset(now).totalSeconds / 60
        )
    }

    private fun json(payload: Map<String, Any?>): String =
        JSONObject(payload.filterValues { it != null }).toString()

    /**
     * Send Firebase config to ESP32 via BLE so it can poll notifications.
     * Sends secretKey (per-device auto-generated) — no manual input needed.
     * ESP32 reads from Firebase path: live/{uid}/{deviceId}/{secretKey}/notification
     */
    fun sendFirebaseConfig() {
        scope.launch {
            val settings = settingsRepository.settings.first()
            val deviceId = settings.deviceId ?: return@launch
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val uid = user.uid
            val secret = credentials.getOrCreate().secretKey

            // Get Firebase Database URL
            val db = FirebaseDatabase.getInstance()
            val dbUrl = db.reference.root.toString().trimEnd('/')

            val configJson = JSONObject().apply {
                put("url", dbUrl)
                put("uid", uid)
                put("deviceId", deviceId)
                put("secret", secret)
            }.toString()

            Log.d("TransportRouter", "Sending Firebase config: url=$dbUrl uid=$uid device=$deviceId")
            ble.send(PacketType.FIREBASE_CONFIG, configJson)
        }
    }

    /**
     * Send clear config command to ESP32 via BLE.
     * ESP32 clears Firebase config from NVS.
     */
    fun sendClearConfig() {
        scope.launch {
            ble.send(PacketType.CLEAR_CONFIG, "{}")
        }
    }

    /**
     * Send WiFi config to ESP32 via BLE.
     * ESP32 saves SSID and password to NVS.
     */
    fun sendWifiConfig(ssid: String, password: String) {
        scope.launch {
            val configJson = JSONObject().apply {
                put("ssid", ssid)
                put("password", password)
            }.toString()
            Log.d("TransportRouter", "Sending WiFi config: ssid=$ssid")
            ble.send(PacketType.WIFI_CONFIG, configJson)
        }
    }

    /**
     * Send switch mode command to ESP32 via BLE.
     * ESP32 saves mode to NVS and restarts.
     */
    fun sendSwitchMode(mode: String) {
        scope.launch {
            val configJson = JSONObject().apply {
                put("mode", mode)
            }.toString()
            Log.d("TransportRouter", "Sending switch mode: $mode")
            ble.send(PacketType.SWITCH_MODE, configJson)
        }
    }
}
