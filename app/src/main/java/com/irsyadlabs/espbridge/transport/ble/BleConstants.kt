package com.irsyadlabs.espbridge.transport.ble

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("7c9e0001-6f2f-4d4d-9f25-0d7fd4f0a001")
    val PHONE_TO_ESP_UUID: UUID = UUID.fromString("7c9e0002-6f2f-4d4d-9f25-0d7fd4f0a001")
    val ESP_TO_PHONE_UUID: UUID = UUID.fromString("7c9e0003-6f2f-4d4d-9f25-0d7fd4f0a001")
    val CCC_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val DEFAULT_MTU = 185
    const val SCAN_PERIOD_MS = 10_000L
    const val TRUSTED_SCAN_PERIOD_MS = 12_000L
}

enum class PacketType(val code: Byte) {
    TIME_SYNC(0x01),
    PHONE_STATUS(0x02),
    NETWORK_STATUS(0x03),
    WEATHER(0x04),
    NOTIFICATION(0x05),
    NAVIGATION(0x06),
    LOCATION(0x07),
    COMMAND(0x08),
    DEVICE_STATUS(0x09),
    ACK(0x0A),
    SYNC_BEGIN(0x0B),
    SYNC_END(0x0C),
    OTA_BEGIN(0x0D),
    OTA_CHUNK(0x0E),
    OTA_END(0x0F),
    OTA_ABORT(0x10),
    OTA_ENTER_RECOVERY(0x11),
    FIREBASE_CONFIG(0x12),
    CLEAR_CONFIG(0x13),
    WIFI_CONFIG(0x14),
    SWITCH_MODE(0x15),
    WIFI_SCAN(0x16),
    WIFI_LIST(0x17),
    FIREBASE_STATUS(0x18);

    companion object {
        fun fromCode(code: Byte): PacketType? = entries.firstOrNull { it.code == code }
    }
}
