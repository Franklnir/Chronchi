package com.irsyadlabs.espbridge.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.irsyadlabs.espbridge.BuildConfig
import com.irsyadlabs.espbridge.core.model.DisplayEvent
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import com.irsyadlabs.espbridge.core.model.PhoneState
import kotlinx.coroutines.tasks.await

class FirebaseCloudTransport(private val context: Context) {
    fun available(): Boolean = BuildConfig.FIREBASE_CONFIG_PRESENT &&
        FirebaseApp.getApps(context).isNotEmpty() && FirebaseAuth.getInstance().currentUser != null && isOnline()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun registerDevice(deviceId: String, displayName: String = "ESP Display") {
        if (!available()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference.child("devices").child(deviceId).updateChildren(
            mapOf(
                "ownerUid" to uid,
                "displayName" to displayName,
                "enabled" to true,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun pushLatest(deviceId: String, key: String, value: Any?, secret: String = "") {
        if (!available()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val path = if (secret.isNotEmpty()) "$deviceId/$secret/$key" else "$deviceId/$key"
        FirebaseDatabase.getInstance().reference
            .child("live")
            .child(uid)
            .child(path)
            .setValue(value)
            .await()
    }

    suspend fun pushSnapshot(deviceId: String, state: PhoneState, secret: String = "") {
        if (!available()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val payload = mapOf(
            "status" to mapOf(
                "online" to true,
                "transport" to "cloud",
                "updatedAt" to System.currentTimeMillis()
            ),
            "time" to mapOf("epochSeconds" to System.currentTimeMillis() / 1000),
            "phone" to state.phoneStatus,
            "network" to mapOf(
                "transport" to state.phoneStatus.networkTransport,
                "generation" to state.phoneStatus.networkGeneration,
                "signalLevel" to state.phoneStatus.signalLevel,
                "updatedAt" to state.phoneStatus.updatedAt
            ),
            "weather" to state.weather,
            "location" to state.location,
            "navigation" to navigationMap(state.navigation),
            "notification" to state.liveData.event?.let(::displayEventMap),
            "updatedAt" to System.currentTimeMillis()
        )
        val basePath = if (secret.isNotEmpty()) "$deviceId/$secret" else deviceId
        FirebaseDatabase.getInstance().reference
            .child("live")
            .child(uid)
            .child(basePath)
            .setValue(payload)
            .await()
    }

    private fun displayEventMap(event: DisplayEvent): Map<String, Any?> = mapOf(
        "sourceApp" to event.sourceApp,
        "category" to event.category.name,
        "primaryText" to event.primaryText,
        "secondaryText" to event.secondaryText,
        "tertiaryText" to event.tertiaryText,
        "timestamp" to event.timestamp
    ).filterValues { it != null }

    private fun navigationMap(value: NavigationDisplayState): Map<String, Any?> = mapOf(
        "active" to value.active,
        "maneuver" to value.maneuver.name,
        "distanceMeters" to value.distanceMeters,
        "distanceText" to value.distanceText,
        "roadName" to value.roadName,
        "destinationDistanceText" to value.destinationDistanceText,
        "time" to value.time,
        "updatedAt" to value.updatedAt
    ).filterValues { it != null }
}
