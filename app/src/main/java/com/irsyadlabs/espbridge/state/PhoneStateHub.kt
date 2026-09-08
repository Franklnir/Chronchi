package com.irsyadlabs.espbridge.state

import com.irsyadlabs.espbridge.core.model.BleDeliveryState
import com.irsyadlabs.espbridge.core.model.DisplayEvent
import com.irsyadlabs.espbridge.core.model.LocationState
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import com.irsyadlabs.espbridge.core.model.PhoneState
import com.irsyadlabs.espbridge.core.model.PhoneStatusState
import com.irsyadlabs.espbridge.core.model.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PhoneStateHub {
    private val _state = MutableStateFlow(PhoneState())
    val state: StateFlow<PhoneState> = _state.asStateFlow()

    fun updateDisplayEvent(value: DisplayEvent, rawPreview: String) = _state.update {
        it.copy(
            liveData = it.liveData.copy(
                event = value,
                rawPreview = rawPreview,
                bleDelivery = BleDeliveryState.PREVIEW_ONLY
            )
        )
    }

    fun updateBleDelivery(timestamp: Long, delivery: BleDeliveryState) = _state.update { state ->
        if (state.liveData.event?.timestamp != timestamp) state
        else state.copy(liveData = state.liveData.copy(bleDelivery = delivery))
    }
    fun updateNavigation(value: NavigationDisplayState) = _state.update { it.copy(navigation = value) }
    fun updateNavigationBleDelivery(updatedAt: Long, delivery: BleDeliveryState) = _state.update { state ->
        if (state.navigation.updatedAt != updatedAt) state
        else state.copy(navigation = state.navigation.copy(bleDelivery = delivery))
    }
    fun updateLocation(value: LocationState) = _state.update { it.copy(location = value) }
    fun updateWeather(value: WeatherState) = _state.update { state ->
        state.copy(
            weather = value,
            homeDisplay = state.homeDisplay.copy(
                temperature = value.temperatureC?.let { "${it.toInt()}C" },
                locationName = value.locationLabel.takeIf(String::isNotBlank)
            )
        )
    }

    fun updatePhoneStatus(value: PhoneStatusState) = _state.update { state ->
        state.copy(
            phoneStatus = value,
            homeDisplay = state.homeDisplay.copy(
                batteryLevel = value.batteryLevel,
                networkType = value.networkGeneration.ifBlank {
                    when (value.networkTransport) {
                        "wifi" -> "WiFi"
                        "cellular" -> "Cellular"
                        else -> null
                    }
                },
                signalLevel = value.signalLevel,
                wifiConnected = value.networkTransport == "wifi",
                bluetoothConnected = value.espConnected
            )
        )
    }
}
