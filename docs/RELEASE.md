# ESPBridge Android release process

ESPBridge 1.2.1 supports a production signing key without storing credentials in the repository. Keep the keystore and passwords in a password manager, CI secret store, or HSM-backed signing service. Losing this key prevents future updates to an installed production app.

Set these environment variables only in the release shell or CI secret context:

```powershell
$env:ESPBRIDGE_RELEASE_STORE_FILE='D:\secure\espbridge-release.p12'
$env:ESPBRIDGE_RELEASE_STORE_PASSWORD='<secret>'
$env:ESPBRIDGE_RELEASE_KEY_ALIAS='espbridge'
$env:ESPBRIDGE_RELEASE_KEY_PASSWORD='<secret>'
# Current ESP32-C3 release: do not set ESPBRIDGE_OTA_MANIFEST_URL.
# Add it only when a future board has been explicitly implemented and allowlisted.
```

Then build with JDK 21:

```powershell
.\gradlew.bat clean testReleaseUnitTest assembleRelease bundleRelease --no-daemon
```

The build enables APK Signature Scheme v1, v2, v3, and v4. Do not distribute an unsigned release or the debug APK. Verify the resulting APK with `apksigner verify --verbose --print-certs` and archive the checksum, mapping file, signed manifest, and source revision for every release.

ESP32-C3 firmware updates from Android are disabled by product policy; service
those units through USB. Do not configure a manifest URL until a future board
has been explicitly implemented, tested, and allowlisted. At that time, use a
real HTTPS object URL and sign a fresh schema-2 manifest. The Android application
verifies the manifest signature; device recovery separately verifies the firmware signature.
