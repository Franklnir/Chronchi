package com.irsyadlabs.espbridge.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.irsyadlabs.espbridge.core.model.LocationState
import com.irsyadlabs.espbridge.data.weather.WeatherRepository
import com.irsyadlabs.espbridge.state.PhoneStateHub
import com.irsyadlabs.espbridge.transport.TransportRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationCollector(
    private val context: Context,
    private val hub: PhoneStateHub,
    private val router: TransportRouter,
    private val weatherRepository: WeatherRepository
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun refresh(includeWeather: Boolean = true) = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext

        val location = runCatching {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
        }.getOrNull() ?: return@withContext

        val state = LocationState(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            speedMps = if (location.hasSpeed()) location.speed else null,
            bearingDegrees = if (location.hasBearing()) location.bearing else null,
            updatedAt = System.currentTimeMillis()
        )
        hub.updateLocation(state)
        router.sendLocation(state)

        if (includeWeather) {
            val weather = runCatching {
                weatherRepository.fetch(location.latitude, location.longitude)
            }.getOrNull()
            if (weather != null) {
                hub.updateWeather(weather)
                router.sendWeather(weather)
            }
        }
    }
}
