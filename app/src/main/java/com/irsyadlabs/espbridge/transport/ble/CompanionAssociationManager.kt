package com.irsyadlabs.espbridge.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid

class CompanionAssociationManager(private val context: Context) {
    private val manager: CompanionDeviceManager? = context.getSystemService(CompanionDeviceManager::class.java)

    fun supported(): Boolean = manager != null

    @SuppressLint("MissingPermission")
    fun requestAssociation(onChooser: (IntentSender) -> Unit, onFailure: (String) -> Unit) {
        val cdm = manager ?: return onFailure("Companion Device Manager is not supported on this device.")
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val deviceFilter = BluetoothLeDeviceFilter.Builder()
            .setScanFilter(scanFilter)
            .build()
        val request = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)
            .build()

        cdm.associate(request, object : CompanionDeviceManager.Callback() {
            @Deprecated("Renamed on Android 13; kept for API 26+ compatibility")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onChooser(chooserLauncher)
            }

            override fun onFailure(error: CharSequence?) {
                onFailure(error?.toString() ?: "Companion association failed.")
            }
        }, null)
    }

    @Suppress("DEPRECATION")
    fun disassociate(address: String) {
        val cdm = manager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cdm.myAssociations
                    .filter { it.deviceMacAddress?.toString().equals(address, ignoreCase = true) }
                    .forEach { cdm.disassociate(it.id) }
            } else {
                cdm.disassociate(address)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun startObservingPresence(address: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager?.startObservingDevicePresence(address) }
        }
    }

    @Suppress("DEPRECATION")
    fun extractDevice(data: Intent?): DiscoveredBleDevice? {
        if (data == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val info = data.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
            val address = info?.deviceMacAddress?.toString()
            if (!address.isNullOrBlank()) {
                return DiscoveredBleDevice(address, info.displayName?.toString() ?: "ESP Device", 0)
            }
            val scan = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, ScanResult::class.java)
            if (scan != null) {
                val name = runCatching { scan.device.name }.getOrNull() ?: scan.scanRecord?.deviceName ?: "ESP Device"
                return DiscoveredBleDevice(scan.device.address, name, scan.rssi)
            }
        } else {
            val scan = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) as? ScanResult
            if (scan != null) {
                val name = runCatching { scan.device.name }.getOrNull() ?: scan.scanRecord?.deviceName ?: "ESP Device"
                return DiscoveredBleDevice(scan.device.address, name, scan.rssi)
            }
        }
        return null
    }
}
