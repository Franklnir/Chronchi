package com.irsyadlabs.espbridge.ui.preview

import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.BleDeliveryState
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.core.model.DisplayEvent
import com.irsyadlabs.espbridge.core.model.DisplayCategory
import com.irsyadlabs.espbridge.core.model.Maneuver
import com.irsyadlabs.espbridge.core.model.LiveDataState
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import com.irsyadlabs.espbridge.core.model.PhoneState
import com.irsyadlabs.espbridge.core.model.PhoneStatusState
import com.irsyadlabs.espbridge.core.model.WeatherState
import com.irsyadlabs.espbridge.core.security.DeviceCredentialsManager
import com.irsyadlabs.espbridge.data.local.LocalSettings
import com.irsyadlabs.espbridge.transport.ble.DiscoveredBleDevice

internal val previewUiState = MainUiState(
    initialized = true,
    signedIn = true,
    email = "preview@espbridge.local",
    firebaseReady = false,
    settings = LocalSettings(
        onboardingComplete = true,
        trustedDeviceAddress = "AA:BB:CC:DD:EE:FF",
        trustedDeviceName = "ESP Bridge Display",
        deviceId = "ESP-PREVIEW"
    ),
    phoneState = PhoneState(
        liveData = LiveDataState(
            event = DisplayEvent(
                sourceApp = "WhatsApp",
                category = DisplayCategory.MESSAGE,
                primaryText = "Budi",
                secondaryText = "Sudah sampai belum?",
                timestamp = 1_777_222_872_000L
            ),
            rawPreview = "Budi\nSudah sampai belum?",
            bleDelivery = BleDeliveryState.SENT
        ),
        navigation = NavigationDisplayState(
            active = true,
            maneuver = Maneuver.RIGHT,
            distanceMeters = 350,
            distanceText = "350 m",
            roadName = "Jalan Sudirman",
            destinationDistanceText = "Tujuan 8 km",
            time = "10:25"
        ),
        weather = WeatherState(
            temperatureC = 29.0,
            condition = "partly_cloudy",
            humidity = 76,
            locationLabel = "Jakarta"
        ),
        phoneStatus = PhoneStatusState(
            batteryLevel = 78,
            charging = true,
            networkTransport = "wifi",
            signalLevel = 4,
            bluetoothEnabled = true,
            espConnected = true
        )
    ),
    bleState = ConnectionState.CONNECTED,
    bleDevices = listOf(
        DiscoveredBleDevice("AA:BB:CC:DD:EE:FF", "ESP Bridge Display", -48)
    ),
    bleRssi = -48,
    credentials = DeviceCredentialsManager.Credentials(
        deviceId = "ESP-PREVIEW",
        accessKey = "AK_PREVIEW",
        secretKey = "SK_PREVIEW_ONLY"
    )
)
