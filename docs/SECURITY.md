# Security and privacy decisions

1. Passwords are handled by Firebase Authentication and never written to Realtime Database.
2. API/device secrets use Android Keystore-backed AES/GCM local encryption.
3. Firebase RTDB defaults to deny-all outside explicitly scoped user/device paths.
4. `device_api` is denied to mobile clients; Firebase Admin Functions provision/read it.
5. HTTPS/cloud is optional. Offline BLE does not require Firebase.
6. Notification, GPS and navigation history are not intentionally stored.
7. Notification access is opt-in and per-source forwarding can be disabled.
8. The app does not request contacts, SMS, call-log, accessibility or file-storage permissions.
9. The app does not read another application's private database. It only receives Android notification events exposed through NotificationListenerService.
10. `android:usesCleartextTraffic="false"` blocks ordinary cleartext HTTP from the app.
11. Backups are disabled for application data and secret preferences are excluded from device transfer.
12. Production ESP32 firmware should require BLE bonding/LE Secure Connections and encrypted GATT access for notification content.
13. Force Stop is respected. The app does not attempt to bypass Android process controls.
