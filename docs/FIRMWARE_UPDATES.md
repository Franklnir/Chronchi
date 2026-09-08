# Firmware updates in ESPBridge Android

## Current product policy

ESPBridge Android **does not permit application-initiated firmware updates for
ESP32-C3 Mini 4 MB**. The Firmware Update screen and verified updater module are
retained, but the current board allowlist is intentionally empty. ESP32-S3 N16R8
support is reserved for a future release and is not implemented or enabled now.

The restriction is enforced before the manifest check, again before download,
and at the BLE transfer boundary. A device cannot enable itself merely by
advertising OTA capability. Existing ESP32-C3 units must be serviced through
USB flashing by a technician using the production package.

## Retained updater architecture

When a future board is explicitly released, Settings may show an update only
when the connected device reports all of the following:

- expected production board
- encrypted BLE required
- `update=recovery`
- an image capacity large enough for the signed release

The retained module verifies the schema-2 HTTPS manifest and downloaded image, commands the
main app to restart into recovery, reconnects to the same trusted device, then
transfers the image. Recovery independently verifies `deviceSignature` before
erasing the main partition. A disconnect fails closed and can be retried because
the permanent recovery image is not overwritten.

Manifest signing fields are:

```text
Manifest signature:
2
<board>
<version>
<size>
<lowercase sha256>
<https firmware URL>
<true|false mandatory>
<single-line release notes>
<deviceSignature>

Device signature:
ESPBridge-OTA-v1
<board>
<version>
<size>
<lowercase sha256>
```

Leave `ESPBRIDGE_OTA_MANIFEST_URL` unset for the current ESP32-C3 release. When a
supported board is released, set it in protected release/CI configuration. The
ECDSA public key is safe to embed and must match the key compiled into recovery.
The private key must never be placed in the Android project or firmware source.

## Automatic reconnection

After a successful encrypted pairing, ESPBridge stores only that trusted device
address and asks Android Companion Device Manager to observe it. The foreground
connection service performs a filtered scan for that address with exponential
backoff, then connects immediately when the ESP32 advertises after power-on.

A device is not saved as trusted until the GATT handshake and device-information
response complete. Choosing Forget Device clears the app association and trusted
address, so automatic reconnection stops. A new/manual pairing is required after
Forget Device or for a device that has never been paired.
