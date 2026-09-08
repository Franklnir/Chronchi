package com.irsyadlabs.espbridge.transport.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BlePacketCodecTest {
    @Test
    fun `frame header and UTF-8 payload match ESPBridge V1`() {
        val json = """{"sourceApp":"WhatsApp","primaryText":"Budi"}"""
        val packet = BlePacketCodec.encodePacket(PacketType.NOTIFICATION, json, mtu = 185)
        val frame = packet.frames.single()

        assertEquals(BlePacketCodec.MAGIC, frame[0])
        assertEquals(BlePacketCodec.VERSION, frame[1])
        assertEquals(PacketType.NOTIFICATION.code, frame[2])
        assertEquals(packet.sequence, frame[3].toInt() and 0xFF)
        assertEquals(0, frame[4].toInt() and 0xFF)
        assertEquals(1, frame[5].toInt() and 0xFF)
        val declared = ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
        assertEquals(frame.size - BlePacketCodec.HEADER_SIZE, declared)
    }

    @Test
    fun `decoder reassembles ordered fragments`() {
        val json = """{"category":"MESSAGE","secondaryText":"${"halo ".repeat(50)}"}"""
        val encoded = BlePacketCodec.encodePacket(PacketType.NOTIFICATION, json, mtu = 23)
        assertTrue(encoded.frames.size > 1)

        val decoder = BlePacketCodec.Decoder()
        encoded.frames.dropLast(1).forEach { frame ->
            assertEquals(BleDecodeResult.Incomplete, decoder.feed(frame))
        }
        val result = decoder.feed(encoded.frames.last()) as BleDecodeResult.Complete
        assertEquals(PacketType.NOTIFICATION, result.packet.type)
        assertEquals(encoded.sequence, result.packet.sequence)
        assertEquals(json, result.packet.jsonPayload)
    }

    @Test
    fun `decoder rejects invalid magic and out-of-order fragments`() {
        val encoded = BlePacketCodec.encodePacket(
            PacketType.NAVIGATION,
            """{"roadName":"${"Jl. Panjang ".repeat(20)}"}""",
            mtu = 23
        )
        val badMagic = encoded.frames.first().clone().also { it[0] = 0x00 }
        assertTrue(BlePacketCodec.Decoder().feed(badMagic) is BleDecodeResult.Error)

        val decoder = BlePacketCodec.Decoder()
        assertTrue(decoder.feed(encoded.frames[1]) is BleDecodeResult.Error)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encoder enforces firmware JSON capacity`() {
        BlePacketCodec.encodePacket(
            PacketType.NOTIFICATION,
            "x".repeat(BlePacketCodec.MAX_JSON_BYTES + 1)
        )
    }

    @Test
    fun `OTA binary frame preserves offset and bytes within negotiated MTU`() {
        val payload = ByteBuffer.allocate(4 + 32)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(0x10203040)
            .put(ByteArray(32) { it.toByte() })
            .array()
        val packet = BlePacketCodec.encodeBinaryPacket(PacketType.OTA_CHUNK, payload, mtu = 185)
        val frame = packet.frames.single()

        assertEquals(PacketType.OTA_CHUNK.code, frame[2])
        assertEquals(0, frame[4].toInt() and 0xFF)
        assertEquals(1, frame[5].toInt() and 0xFF)
        assertEquals(payload.size, ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF))
        assertTrue(payload.contentEquals(frame.copyOfRange(BlePacketCodec.HEADER_SIZE, frame.size)))
        assertTrue(frame.size <= 182)
    }

    @Test
    fun `OTA packet codes remain compatible with firmware contract`() {
        assertEquals(PacketType.OTA_BEGIN, PacketType.fromCode(0x0D))
        assertEquals(PacketType.OTA_CHUNK, PacketType.fromCode(0x0E))
        assertEquals(PacketType.OTA_END, PacketType.fromCode(0x0F))
        assertEquals(PacketType.OTA_ABORT, PacketType.fromCode(0x10))
        assertEquals(PacketType.OTA_ENTER_RECOVERY, PacketType.fromCode(0x11))
    }
}
