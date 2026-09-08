package com.irsyadlabs.espbridge.collector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.irsyadlabs.espbridge.core.model.PhoneStatusState
import com.irsyadlabs.espbridge.state.PhoneStateHub
import com.irsyadlabs.espbridge.transport.TransportRouter
import com.irsyadlabs.espbridge.transport.ble.BleConnectionManager

class SystemStateCollector(
    private val context: Context,
    private val hub: PhoneStateHub,
    private val router: TransportRouter,
    private val ble: BleConnectionManager
) {
    @SuppressLint("MissingPermission")
    fun refresh() {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (scale > 0) level * 100 / scale else level
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let(cm::getNetworkCapabilities)
        val transport = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ethernet"
            else -> "offline"
        }

        var generation = ""
        var signalLevel = 0
        if (transport == "wifi") {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
            @Suppress("DEPRECATION")
            val rssi = wifi.connectionInfo?.rssi ?: -100
            signalLevel = WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
            generation = "Wi-Fi"
        } else if (transport == "cellular" && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephony = context.getSystemService(TelephonyManager::class.java)
            generation = networkGeneration(runCatching { telephony.dataNetworkType }.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                signalLevel = runCatching { telephony.signalStrength?.level ?: 0 }.getOrDefault(0).coerceIn(0, 4)
            }
        }

        val btEnabled = context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        val value = PhoneStatusState(
            batteryLevel = batteryPercent.coerceIn(0, 100),
            charging = charging,
            networkTransport = transport,
            networkGeneration = generation,
            signalLevel = signalLevel,
            bluetoothEnabled = btEnabled,
            espConnected = ble.isConnected(),
            updatedAt = System.currentTimeMillis()
        )
        hub.updatePhoneStatus(value)
        router.sendPhoneStatus(value)
    }

    private fun networkGeneration(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_GSM -> "2G"
        else -> "Cellular"
    }
}
