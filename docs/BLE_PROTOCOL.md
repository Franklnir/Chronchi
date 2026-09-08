# ESP Bridge BLE Protocol v1

## Roles
- Android: BLE Central / GATT Client.
- ESP32: BLE Peripheral / GATT Server.

## GATT UUIDs
- Service: `7c9e0001-6f2f-4d4d-9f25-0d7fd4f0a001`
- Phone → ESP32 write characteristic: `7c9e0002-6f2f-4d4d-9f25-0d7fd4f0a001`
- ESP32 → Phone notify characteristic: `7c9e0003-6f2f-4d4d-9f25-0d7fd4f0a001`

The ESP32 must advertise the service UUID above. The Android scanner filters on this UUID.

## Framing
Each GATT write contains an 8-byte header followed by UTF-8 JSON. `OTA_BEGIN`
and `OTA_CHUNK` use the same header with a binary payload.

| Byte | Meaning |
|---|---|
| 0 | Magic `0x45` (`E`) |
| 1 | Protocol version `0x01` |
| 2 | Packet type |
| 3 | Sequence 0–255 |
| 4 | Fragment index |
| 5 | Fragment count |
| 6–7 | Big-endian payload length in this fragment |
| 8… | UTF-8 JSON payload |

Fragments with the same sequence must be concatenated before JSON parsing.

## Packet types
- `0x01` TIME_SYNC
- `0x02` PHONE_STATUS
- `0x03` NETWORK_STATUS
- `0x04` WEATHER
- `0x05` NOTIFICATION
- `0x06` NAVIGATION
- `0x07` LOCATION
- `0x08` COMMAND
- `0x09` DEVICE_STATUS
- `0x0A` ACK
- `0x0B` SYNC_BEGIN
- `0x0C` SYNC_END
- `0x0D` OTA_BEGIN
- `0x0E` OTA_CHUNK
- `0x0F` OTA_END
- `0x10` OTA_ABORT
- `0x11` OTA_ENTER_RECOVERY

## 4 MB recovery update

The main firmware accepts `OTA_ENTER_RECOVERY`, persists the update state,
selects the permanent factory recovery partition, acknowledges, and restarts.
Android must reconnect and confirm `DEVICE_STATUS.role == "recovery"` before
sending `OTA_BEGIN`.

`OTA_BEGIN` is one binary frame containing, in order: big-endian uint32 image
size, 32 raw SHA-256 bytes, uint8 board length plus UTF-8 board, uint8 version
length plus UTF-8 version, and uint8 signature length plus DER ECDSA signature.
Recovery verifies the signature over:

```text
ESPBridge-OTA-v1
<board>
<version>
<size>
<lowercase sha256>
```

Each `OTA_CHUNK` starts with a big-endian uint32 offset followed by image bytes.
Offsets must be exactly sequential. `OTA_END` has JSON `{}` and succeeds only
after full-size, SHA-256, signature, and ESP-IDF image validation.

## Initial synchronization
After GATT service discovery succeeds:
1. `SYNC_BEGIN`
2. `TIME_SYNC`
3. `PHONE_STATUS`
4. `NETWORK_STATUS`
5. current `WEATHER`, if available
6. current `LOCATION`, if enabled
7. current `NAVIGATION`
8. latest `NOTIFICATION`, if any
9. `SYNC_END`

After this, Android sends only the latest changed state.

## Compact notification payload

`NOTIFICATION` contains the normalized OLED-facing event, not the full Android notification body:

```json
{
  "sourceApp": "DANA",
  "category": "PAYMENT",
  "primaryText": "+ Rp50.000",
  "secondaryText": "Dari Budi",
  "paymentDirection": "INCOMING",
  "timestamp": 1777222872000
}
```

`secondaryText`, `paymentDirection`, and `orderStatus` are omitted when they are not applicable. The firmware responds on the notify characteristic with a framed `ACK` packet containing the original sequence number. Android only shows **ACK by ESP32** after that acknowledgement is received; a GATT write alone is not treated as successful rendering.

## Firmware acknowledgement

The ESP32 sends packet type `0x0A` using the same 8-byte frame format. After notification subscription is enabled it sends:

```json
{"r":true}
```

For every accepted logical packet (after all fragments are reassembled):

```json
{"ok":true}
```

The original packet sequence is carried in byte 3 of the ACK frame header. Rejected data returns `{"ok":false}`; the detailed rejection reason remains in the ESP32 serial log. These compact control payloads fit the default 23-byte ATT MTU, so READY/ACK does not depend on successful MTU negotiation. Android also accepts the earlier verbose V1 READY/ACK shape for compatibility. The Android connection is considered ready only after the framed ready message is received.

Category values used by the firmware renderer are `MESSAGE`, `PROFESSIONAL`, `PAYMENT`, `ORDER`, `NAVIGATION`, and `SYSTEM`. Android does not send screenshots, bitmap frames, display coordinates, or renderer code.

The structured `NAVIGATION` payload uses `maneuver`, `distanceText`, `roadName`, `destinationDistanceText`, `time`, and `active`. Supported V1 maneuver values are `STRAIGHT`, `LEFT`, `RIGHT`, `SLIGHT_LEFT`, `SLIGHT_RIGHT`, `ROUNDABOUT`, `ARRIVE`, and `UNKNOWN`.

## Security requirement for firmware
For real notification/payment content, do not expose a writable/readable unauthenticated BLE service in production. Configure the ESP32 for BLE bonding / LE Secure Connections and require encryption for sensitive GATT characteristics. Treat the Android-side trusted MAC address as convenience, not as cryptographic identity.
