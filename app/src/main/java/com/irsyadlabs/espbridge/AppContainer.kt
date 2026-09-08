package com.irsyadlabs.espbridge

import android.content.Context
import com.irsyadlabs.espbridge.collector.LocationCollector
import com.irsyadlabs.espbridge.collector.SystemStateCollector
import com.irsyadlabs.espbridge.core.security.DeviceCredentialsManager
import com.irsyadlabs.espbridge.core.security.KeystoreSecretStore
import com.irsyadlabs.espbridge.data.auth.AuthRepository
import com.irsyadlabs.espbridge.data.firebase.FirebaseCloudTransport
import com.irsyadlabs.espbridge.data.firmware.FirmwareUpdateManager
import com.irsyadlabs.espbridge.data.firmware.FirmwareUpdateRepository
import com.irsyadlabs.espbridge.data.local.SettingsRepository
import com.irsyadlabs.espbridge.data.weather.WeatherRepository
import com.irsyadlabs.espbridge.state.PhoneStateHub
import com.irsyadlabs.espbridge.transport.TransportRouter
import com.irsyadlabs.espbridge.transport.ble.BleConnectionManager
import com.irsyadlabs.espbridge.transport.ble.CompanionAssociationManager

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsRepository(appContext)
    val secretStore = KeystoreSecretStore(appContext)
    val credentials = DeviceCredentialsManager(settings, secretStore)
    val auth = AuthRepository(appContext, settings)
    val stateHub = PhoneStateHub()
    val ble = BleConnectionManager(appContext)
    val firmwareUpdates = FirmwareUpdateManager(FirmwareUpdateRepository(), ble)
    val companionAssociation = CompanionAssociationManager(appContext)
    val cloud = FirebaseCloudTransport(appContext)
    val router = TransportRouter(settings, ble, cloud, stateHub, credentials)
    val weather = WeatherRepository(settings)
    val systemCollector = SystemStateCollector(appContext, stateHub, router, ble)
    val locationCollector = LocationCollector(appContext, stateHub, router, weather)
}
