package com.irsyadlabs.espbridge.core.model

data class FirmwareUpdateAccess(
    val allowed: Boolean,
    val reason: String
)

fun interface FirmwareUpdateAccessPolicy {
    fun evaluate(board: String, otaAdvertised: Boolean): FirmwareUpdateAccess
}

/**
 * Product-level allowlist for firmware updates initiated by ESPBridge Android.
 *
 * The device capability is never sufficient on its own: a board must also be
 * explicitly released here. The current release intentionally enables no board.
 * ESP32-S3 N16R8 support will be added only with its future firmware integration.
 */
object AndroidFirmwareUpdatePolicy : FirmwareUpdateAccessPolicy {
    const val C3_BLOCK_REASON =
        "Update dari aplikasi dinonaktifkan untuk ESP32-C3 Mini 4 MB. " +
            "Firmware hanya boleh diservis melalui flash USB."
    const val NOT_RELEASED_REASON =
        "Board ini belum diizinkan untuk update dari aplikasi. " +
            "Dukungan ESP32-S3 N16R8 belum diaktifkan pada rilis ini."

    // Deliberately empty. Future board support requires an explicit, reviewed release change.
    private val enabledBoards: Set<String> = emptySet()

    override fun evaluate(board: String, otaAdvertised: Boolean): FirmwareUpdateAccess {
        val normalizedBoard = board.trim().lowercase()
        val isEsp32C3 = normalizedBoard.startsWith("esp32c3") ||
            normalizedBoard.startsWith("esp32-c3")
        if (isEsp32C3) return FirmwareUpdateAccess(false, C3_BLOCK_REASON)
        if (!otaAdvertised) {
            return FirmwareUpdateAccess(
                false,
                "Perangkat belum menyediakan updater firmware yang aman."
            )
        }
        if (normalizedBoard !in enabledBoards) {
            return FirmwareUpdateAccess(false, NOT_RELEASED_REASON)
        }
        return FirmwareUpdateAccess(true, "Update dari aplikasi diizinkan untuk board ini.")
    }
}
