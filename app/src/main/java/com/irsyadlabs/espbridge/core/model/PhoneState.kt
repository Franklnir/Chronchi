package com.irsyadlabs.espbridge.core.model

data class HomeDisplayState(
    val time: String = "--:--",
    val date: String = "",
    val temperature: String? = null,
    val locationName: String? = null,
    val batteryLevel: Int = 0,
    val networkType: String? = null,
    val signalLevel: Int? = null,
    val wifiConnected: Boolean = false,
    val bluetoothConnected: Boolean = false
)

enum class Maneuver {
    STRAIGHT,
    LEFT,
    RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    ROUNDABOUT,
    ARRIVE,
    UNKNOWN
}

data class NavigationDisplayState(
    val active: Boolean = false,
    val maneuver: Maneuver = Maneuver.UNKNOWN,
    val distanceMeters: Int? = null,
    val distanceText: String = "",
    val roadName: String = "",
    val destinationDistanceText: String? = null,
    val time: String = "",
    val updatedAt: Long = 0L,
    val bleDelivery: BleDeliveryState = BleDeliveryState.PREVIEW_ONLY
)

data class LocationState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val updatedAt: Long = 0L
)

data class WeatherState(
    val temperatureC: Double? = null,
    val condition: String = "unknown",
    val humidity: Int? = null,
    val locationLabel: String = "",
    val updatedAt: Long = 0L,
    val isCached: Boolean = false
)

data class PhoneStatusState(
    val batteryLevel: Int = 0,
    val charging: Boolean = false,
    val networkTransport: String = "offline",
    val networkGeneration: String = "",
    val signalLevel: Int = 0,
    val bluetoothEnabled: Boolean = false,
    val espConnected: Boolean = false,
    val updatedAt: Long = 0L
)

data class PhoneState(
    val liveData: LiveDataState = LiveDataState(),
    val homeDisplay: HomeDisplayState = HomeDisplayState(),
    val navigation: NavigationDisplayState = NavigationDisplayState(),
    val location: LocationState = LocationState(),
    val weather: WeatherState = WeatherState(),
    val phoneStatus: PhoneStatusState = PhoneStatusState()
)
