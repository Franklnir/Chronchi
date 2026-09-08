package com.irsyadlabs.espbridge.data.firmware

import com.irsyadlabs.espbridge.BuildConfig
import com.irsyadlabs.espbridge.core.model.AndroidFirmwareUpdatePolicy
import com.irsyadlabs.espbridge.core.model.FirmwareUpdateAccessPolicy
import com.irsyadlabs.espbridge.transport.ble.ConnectedDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class FirmwareManifest(
    val schema: Int,
    val board: String,
    val version: String,
    val size: Long,
    val sha256: String,
    val url: String,
    val mandatory: Boolean,
    val notes: String,
    val deviceSignature: String,
    val signature: String
) {
    fun canonicalPayload(): ByteArray = listOf(
        schema.toString(),
        board,
        version,
        size.toString(),
        sha256.lowercase(),
        url,
        mandatory.toString(),
        notes,
        deviceSignature
    ).joinToString("\n").toByteArray(Charsets.UTF_8)
}

sealed interface FirmwareCheckResult {
    data class Available(val manifest: FirmwareManifest) : FirmwareCheckResult
    data class Current(val version: String) : FirmwareCheckResult
    data class Unavailable(val reason: String) : FirmwareCheckResult
}

class FirmwareUpdateRepository(
    private val manifestUrl: String = BuildConfig.OTA_MANIFEST_URL,
    private val publicKeyBase64: String = BuildConfig.OTA_PUBLIC_KEY_B64,
    private val accessPolicy: FirmwareUpdateAccessPolicy = AndroidFirmwareUpdatePolicy
) {
    fun configured(): Boolean = manifestUrl.isNotBlank() && publicKeyBase64.isNotBlank()

    suspend fun check(device: ConnectedDeviceInfo): FirmwareCheckResult = withContext(Dispatchers.IO) {
        val access = accessPolicy.evaluate(device.board, device.otaAvailable)
        if (!access.allowed) {
            return@withContext FirmwareCheckResult.Unavailable(access.reason)
        }
        if (!configured()) {
            return@withContext FirmwareCheckResult.Unavailable(
                "Kanal rilis belum dikonfigurasi pada build aplikasi."
            )
        }
        val manifest = fetchManifest()
        require(manifest.board == device.board) { "Manifest ditujukan untuk board lain" }
        require(manifest.size in 1..device.maximumImageSize) { "Firmware melebihi slot OTA" }
        require(verifyManifest(manifest)) { "Tanda tangan manifest tidak valid" }
        if (compareVersions(manifest.version, device.firmwareVersion) > 0) {
            FirmwareCheckResult.Available(manifest)
        } else {
            FirmwareCheckResult.Current(device.firmwareVersion)
        }
    }

    suspend fun download(manifest: FirmwareManifest, onProgress: (Float) -> Unit): ByteArray =
        withContext(Dispatchers.IO) {
            require(verifyManifest(manifest)) { "Tanda tangan manifest tidak valid" }
            val connection = openHttps(manifest.url)
            try {
                connection.connect()
                require(connection.responseCode in 200..299) {
                    "Unduhan firmware gagal (HTTP ${connection.responseCode})"
                }
                val declared = connection.contentLengthLong
                if (declared >= 0) require(declared == manifest.size) {
                    "Ukuran unduhan tidak sesuai manifest"
                }
                val output = ByteArrayOutputStream(manifest.size.toInt())
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= manifest.size) { "Unduhan melebihi ukuran manifest" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        onProgress(total.toFloat() / manifest.size.toFloat())
                    }
                }
                val image = output.toByteArray()
                require(image.size.toLong() == manifest.size) { "Unduhan firmware tidak lengkap" }
                val actualHash = digest.digest().joinToString("") {
                    "%02x".format(it.toInt() and 0xff)
                }
                require(actualHash.equals(manifest.sha256, ignoreCase = true)) {
                    "SHA-256 firmware tidak cocok"
                }
                image
            } finally {
                connection.disconnect()
            }
        }

    internal fun verifyManifest(manifest: FirmwareManifest): Boolean {
        if (!configured()) return false
        return runCatching {
            val keyBytes = Base64.getMimeDecoder().decode(publicKeyBase64)
            val publicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(keyBytes))
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(manifest.canonicalPayload())
            verifier.verify(Base64.getMimeDecoder().decode(manifest.signature))
        }.getOrDefault(false)
    }

    private fun fetchManifest(): FirmwareManifest {
        val connection = openHttps(manifestUrl)
        try {
            connection.connect()
            require(connection.responseCode in 200..299) {
                "Pemeriksaan update gagal (HTTP ${connection.responseCode})"
            }
            require(connection.contentLengthLong <= MAX_MANIFEST_BYTES ||
                connection.contentLengthLong < 0) { "Manifest terlalu besar" }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_MANIFEST_BYTES) { "Manifest terlalu besar" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            return FirmwareManifest(
                schema = json.getInt("schema"),
                board = json.getString("board"),
                version = json.getString("version"),
                size = json.getLong("size"),
                sha256 = json.getString("sha256"),
                url = json.getString("url"),
                mandatory = json.optBoolean("mandatory", false),
                notes = json.optString("notes"),
                deviceSignature = json.getString("deviceSignature"),
                signature = json.getString("signature")
            ).also {
                require(it.schema == 2) { "Versi manifest tidak didukung" }
                require(it.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 manifest tidak valid" }
                require(it.version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][0-9A-Za-z.-]+)?"))) {
                    "Versi firmware tidak valid"
                }
                require(it.board.matches(Regex("[0-9A-Za-z._-]{3,64}"))) { "Nama board tidak valid" }
                require(!it.notes.contains('\n') && !it.notes.contains('\r')) {
                    "Catatan rilis tidak boleh memuat baris baru"
                }
                require(runCatching { Base64.getMimeDecoder().decode(it.deviceSignature).size in 64..80 }
                    .getOrDefault(false)) { "Tanda tangan perangkat tidak valid" }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openHttps(value: String): HttpURLConnection {
        val uri = URI(value)
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            "Endpoint firmware wajib memakai HTTPS"
        }
        return (URL(value).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", "ESPBridge-Android/${BuildConfig.VERSION_NAME}")
        }
    }

    companion object {
        private const val MAX_MANIFEST_BYTES = 64 * 1024

        internal fun compareVersions(left: String, right: String): Int {
            fun parts(value: String): List<Int> = value.substringBefore('-').substringBefore('+')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }
            val a = parts(left)
            val b = parts(right)
            for (index in 0 until maxOf(a.size, b.size)) {
                val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
                if (comparison != 0) return comparison
            }
            return 0
        }
    }
}
