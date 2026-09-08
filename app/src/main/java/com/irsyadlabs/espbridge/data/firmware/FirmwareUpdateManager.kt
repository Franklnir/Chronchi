package com.irsyadlabs.espbridge.data.firmware

import com.irsyadlabs.espbridge.core.model.AndroidFirmwareUpdatePolicy
import com.irsyadlabs.espbridge.transport.ble.BleConnectionManager
import com.irsyadlabs.espbridge.transport.ble.ConnectedDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface FirmwareUpdateState {
    data object Idle : FirmwareUpdateState
    data object Checking : FirmwareUpdateState
    data class NotAvailable(val reason: String) : FirmwareUpdateState
    data class Current(val version: String) : FirmwareUpdateState
    data class Available(val manifest: FirmwareManifest) : FirmwareUpdateState
    data class Downloading(val progress: Float) : FirmwareUpdateState
    data class Installing(val progress: Float) : FirmwareUpdateState
    data class Rebooting(val version: String) : FirmwareUpdateState
    data class Failed(val message: String) : FirmwareUpdateState
}

class FirmwareUpdateManager(
    private val repository: FirmwareUpdateRepository,
    private val ble: BleConnectionManager
) {
    private val _state = MutableStateFlow<FirmwareUpdateState>(FirmwareUpdateState.Idle)
    val state: StateFlow<FirmwareUpdateState> = _state.asStateFlow()

    suspend fun check(device: ConnectedDeviceInfo?) {
        if (device == null) {
            _state.value = FirmwareUpdateState.NotAvailable("Hubungkan Chronchi terlebih dahulu.")
            return
        }
        _state.value = FirmwareUpdateState.Checking
        _state.value = runCatching { repository.check(device) }
            .fold(
                onSuccess = { result ->
                    when (result) {
                        is FirmwareCheckResult.Available -> FirmwareUpdateState.Available(result.manifest)
                        is FirmwareCheckResult.Current -> FirmwareUpdateState.Current(result.version)
                        is FirmwareCheckResult.Unavailable -> FirmwareUpdateState.NotAvailable(result.reason)
                    }
                },
                onFailure = { FirmwareUpdateState.Failed(it.message ?: "Pemeriksaan update gagal") }
            )
    }

    suspend fun install() {
        val manifest = (_state.value as? FirmwareUpdateState.Available)?.manifest ?: return
        val device = ble.deviceInfo.value
        if (device == null) {
            _state.value = FirmwareUpdateState.NotAvailable("Hubungkan Chronchi terlebih dahulu.")
            return
        }
        val access = AndroidFirmwareUpdatePolicy.evaluate(device.board, device.otaAvailable)
        if (!access.allowed) {
            _state.value = FirmwareUpdateState.NotAvailable(access.reason)
            return
        }
        try {
            _state.value = FirmwareUpdateState.Downloading(0f)
            val image = repository.download(manifest) { progress ->
                _state.value = FirmwareUpdateState.Downloading(progress.coerceIn(0f, 1f))
            }
            _state.value = FirmwareUpdateState.Installing(0f)
            ble.transferFirmware(
                image = image,
                version = manifest.version,
                sha256 = manifest.sha256,
                board = manifest.board,
                deviceSignature = manifest.deviceSignature
            ) { progress ->
                _state.value = FirmwareUpdateState.Installing(progress.coerceIn(0f, 1f))
            }
            _state.value = FirmwareUpdateState.Rebooting(manifest.version)
        } catch (error: Throwable) {
            _state.value = FirmwareUpdateState.Failed(error.message ?: "Update firmware gagal")
        }
    }
}
