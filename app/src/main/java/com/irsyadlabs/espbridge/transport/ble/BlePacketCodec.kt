package com.irsyadlabs.espbridge.transport.ble

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

data class EncodedBlePacket(
    val type: PacketType,
    val sequence: Int,
    val frames: List<ByteArray>
)

data class DecodedBlePacket(
    val type: PacketType,
    val sequence: Int,
    val jsonPayload: String
)

sealed interface BleDecodeResult {
    data object Incomplete : BleDecodeResult
    data class Complete(val packet: DecodedBlePacket) : BleDecodeResult
    data class Error(val reason: String) : BleDecodeResult
}

/**
 * ESPBridge BLE V1 framing.
 * Header (8 bytes): magic, version, type, sequence, fragmentIndex,
 * fragmentCount, payloadLength (big-endian uint16). Payload is UTF-8 JSON.
 */
object BlePacketCodec {
    const val MAGIC: Byte = 0x45 // 'E'
    const val VERSION: Byte = 0x01
    const val HEADER_SIZE = 8
    const val MAX_JSON_BYTES = 768

    private val sequence = AtomicInteger(0)

    private fun nextSequence(): Int = sequence.updateAndGet { (it + 1) and 0xFF }

    fun encodePacket(
        type: PacketType,
        jsonPayload: String,
        mtu: Int = BleConstants.DEFAULT_MTU
    ): EncodedBlePacket {
        val payload = jsonPayload.toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_JSON_BYTES) {
            "JSON payload exceeds ESPBridge V1 limit ($MAX_JSON_BYTES bytes)"
        }
        val maxGattPayload = (mtu - 3).coerceAtLeast(20)
        val maxChunk = (maxGattPayload - HEADER_SIZE).coerceAtLeast(8)
        val fragmentCount = ((payload.size + maxChunk - 1) / maxChunk).coerceAtLeast(1)
        require(fragmentCount <= 255) { "Payload needs too many BLE fragments" }
        val packetSequence = nextSequence()

        val frames = (0 until fragmentCount).map { index ->
            val start = index * maxChunk
            val end = minOf(start + maxChunk, payload.size)
            val part = payload.copyOfRange(start, end)
            ByteBuffer.allocate(HEADER_SIZE + part.size)
                .order(ByteOrder.BIG_ENDIAN)
                .put(MAGIC)
                .put(VERSION)
                .put(type.code)
                .put(packetSequence.toByte())
                .put(index.toByte())
                .put(fragmentCount.toByte())
                .putShort(part.size.toShort())
                .put(part)
                .array()
        }
        return EncodedBlePacket(type, packetSequence, frames)
    }

    /** Encodes one raw binary frame, used for sequential OTA chunks. */
    fun encodeBinaryPacket(
        type: PacketType,
        payload: ByteArray,
        mtu: Int = BleConstants.DEFAULT_MTU
    ): EncodedBlePacket {
        val maxGattPayload = (mtu - 3).coerceAtLeast(20)
        require(payload.size + HEADER_SIZE <= maxGattPayload) {
            "Binary payload exceeds negotiated BLE MTU"
        }
        val packetSequence = nextSequence()
        val frame = ByteBuffer.allocate(HEADER_SIZE + payload.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .put(VERSION)
            .put(type.code)
            .put(packetSequence.toByte())
            .put(0)
            .put(1)
            .putShort(payload.size.toShort())
            .put(payload)
            .array()
        return EncodedBlePacket(type, packetSequence, listOf(frame))
    }

    /** Retained for callers that only need the GATT frames. */
    fun encode(
        type: PacketType,
        jsonPayload: String,
        mtu: Int = BleConstants.DEFAULT_MTU
    ): List<ByteArray> = encodePacket(type, jsonPayload, mtu).frames

    class Decoder {
        private var expectedType: PacketType? = null
        private var expectedSequence = -1
        private var expectedFragmentCount = 0
        private var nextFragment = 0
        private val payload = ByteArrayOutputStream(MAX_JSON_BYTES)

        @Synchronized
        fun feed(frame: ByteArray): BleDecodeResult {
            if (frame.size < HEADER_SIZE) return error("BLE frame too short")
            if (frame[0] != MAGIC) return error("Invalid BLE frame magic")
            if (frame[1] != VERSION) return error("Unsupported BLE protocol version")

            val type = PacketType.fromCode(frame[2]) ?: return error("Unknown BLE packet type")
            val sequence = frame[3].toInt() and 0xFF
            val fragmentIndex = frame[4].toInt() and 0xFF
            val fragmentCount = frame[5].toInt() and 0xFF
            val declaredLength = ((frame[6].toInt() and 0xFF) shl 8) or
                (frame[7].toInt() and 0xFF)
            if (fragmentCount == 0 || fragmentIndex >= fragmentCount) {
                return error("Invalid BLE fragment metadata")
            }
            if (declaredLength != frame.size - HEADER_SIZE) {
                return error("BLE payload length mismatch")
            }

            if (fragmentIndex == 0) {
                reset()
                expectedType = type
                expectedSequence = sequence
                expectedFragmentCount = fragmentCount
            } else if (
                type != expectedType ||
                sequence != expectedSequence ||
                fragmentCount != expectedFragmentCount
            ) {
                return error("BLE fragment stream changed")
            }
            if (fragmentIndex != nextFragment) return error("BLE fragment out of order")
            if (payload.size() + declaredLength > MAX_JSON_BYTES) {
                return error("BLE JSON payload too large")
            }

            payload.write(frame, HEADER_SIZE, declaredLength)
            nextFragment += 1
            if (nextFragment < expectedFragmentCount) return BleDecodeResult.Incomplete

            val packet = DecodedBlePacket(
                type = requireNotNull(expectedType),
                sequence = expectedSequence,
                jsonPayload = payload.toByteArray().toString(Charsets.UTF_8)
            )
            reset()
            return BleDecodeResult.Complete(packet)
        }

        @Synchronized
        fun reset() {
            expectedType = null
            expectedSequence = -1
            expectedFragmentCount = 0
            nextFragment = 0
            payload.reset()
        }

        private fun error(reason: String): BleDecodeResult.Error {
            reset()
            return BleDecodeResult.Error(reason)
        }
    }
}
