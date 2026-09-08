# ESP Bridge — Android Companion App for ESP32

A Kotlin/Jetpack Compose Android project implementing the companion-app architecture discussed in the PRD: selected Android notifications, Google Maps navigation parsing, GPS, weather, phone/network status, offline Bluetooth LE transport, Firebase latest-state transport, login/register UI, permission onboarding, background reconnect service, and a consistent playful utility UI.

## Open it in Android Studio

1. Extract/open the **ESPBridge** folder as an Android Studio project.
2. Let Gradle sync. The first sync needs internet to download Gradle/Maven dependencies.
3. Select an Android device/emulator with API 26+.
4. Run the `app` configuration.

No Firebase file is required just to launch and inspect the application. Without `app/google-services.json`, the project enters **Preview Mode** for authentication. This lets you immediately inspect Login → Permission onboarding → Home → Setup → Settings.

For real Firebase login/cloud state, follow `docs/FIREBASE_SETUP.md`.

## Product behavior

### Offline
When internet is unavailable:

```text
Android notification / phone state
              ↓
      Normalize compact event
              ↓
          StateHub (RAM)
          /        \
 Live Monitor   Bluetooth LE
                    ↓
                  ESP32
                    ↓
                  OLED
```

Firebase is not required for Bluetooth operation. Cloud writes are gated on validated internet connectivity.

### Online
When online and cloud sync is enabled:

```text
StateHub ─────────────→ BLE → ESP32
   │
   └────────→ Firebase RTDB latest-state
                         ↓
                    Secure API mode
```

The database uses fixed `live/...` paths and overwrites them. It does not call `push()` for notification, GPS or navigation events.

## UI

Design tokens are centralized under `ui/theme` and `ui/components`:
- Blue `#1746D1`
- Yellow `#FFC928`
- Red `#FF3B24`
- Paper white `#FFFDF8`
- Ink black `#111111`
- 16 dp horizontal padding
- 20 dp card radius
- 52 dp primary buttons
- fixed Home / Setup / Settings bottom navigation

The visual direction is a mobile card layout with bold, playful blue/yellow/red accents while security/configuration screens remain clean and readable.

## Main screens

### Login
- Email/password
- Google Sign-In entry point
- Preview Mode fallback until Firebase is configured

### Register
- Email
- Password
- Confirm password (client-side validation only)
- Google entry point

### Permission onboarding
- Notification Listener access
- Bluetooth/Nearby Devices
- Location (optional)
- Phone/network status (optional)
- Background connection guidance

The app deliberately does **not** request contacts, SMS, call log, file-storage or accessibility access.

### Home
Notification source categories:
- Messaging & Social
- Professional: Gmail, LinkedIn, JobStreet
- Payment
- Banking
- Marketplace
- System sources: Navigation, Weather, Location, Phone Status, Network Status

Notification source cards use bundled app icons so scrolling never performs package/icon decoding on the UI thread. A lightweight initial badge is used for a future source without an asset.

### Setup
- connection overview
- Automatic / BLE-only / Cloud-only mode
- BLE scan and trusted-device selection
- live latest-event monitor with normalized ESP output
- monochrome 2:1 OLED preview using HOME, MESSAGE, PROFESSIONAL, PAYMENT, ORDER, NAVIGATION, and SYSTEM templates
- reconnect/disconnect/forget
- BLE RSSI
- device ID/access key/secret key UI
- cloud status

### Settings
- account/profile state
- permission health
- auto reconnect
- background connection
- cloud latest-state toggle
- logout

## Bluetooth firmware contract

The Android app expects the ESP32 to advertise:

- Service UUID: `7c9e0001-6f2f-4d4d-9f25-0d7fd4f0a001`
- Phone → ESP32 write: `7c9e0002-6f2f-4d4d-9f25-0d7fd4f0a001`
- ESP32 → Phone notify: `7c9e0003-6f2f-4d4d-9f25-0d7fd4f0a001`

See `docs/BLE_PROTOCOL.md` for packet framing and packet types.

**Important:** production ESP32 firmware should require secure BLE bonding / LE Secure Connections and encrypted characteristics before displaying private notification/payment content.

## Navigation

V1 listens to Google Maps notification content through `NotificationListenerService` and normalizes guidance into:

```json
{
  "active": true,
  "maneuver": "RIGHT",
  "distanceMeters": 200,
  "distanceText": "200 M",
  "roadName": "Jl. Ahmad Yani",
  "destinationDistanceText": "Tujuan 10 km",
  "time": "10:25"
}
```

`NavigationParser` recognizes common Indonesian and English turn phrases. GPS coordinates are treated separately from turn-by-turn instructions.

## Weather

Weather uses current device location and Open-Meteo. The last value is cached locally so an offline display can show a clearly stale/cached value rather than pretending it is live.

## Background behavior

`DeviceConnectionService` is a `connectedDevice` foreground service. It:
- attempts reconnect to the trusted BLE address
- refreshes phone/network status
- refreshes enabled location/weather periodically
- performs full-state synchronization after BLE reconnect

`BootReceiver` and `BluetoothStateReceiver` restart the connection service when applicable. Android Force Stop is intentionally respected.

A `CompanionDeviceService` hook is included for OS-level companion presence integration. The current MVP trusted-device selection uses BLE service discovery + stored device address; production can additionally wire the Android Companion Device Manager association chooser for stronger OS-level companion lifecycle integration.

## Security notes

Read `docs/SECURITY.md`. Highlights:
- no password stored in RTDB
- Keystore-backed encryption for local device secrets
- deny-by-default RTDB rules
- no history paths for notifications/location/navigation
- no cleartext network traffic
- no broad `QUERY_ALL_PACKAGES`
- package visibility is explicitly scoped
- API credential backend template stores only hashes

## Firebase/API template

`firebase/` includes:
- Realtime Database rules
- Cloud Functions template for device provisioning
- device latest-state GET endpoint

The mobile client already supports Firebase Authentication and RTDB latest-state writes when `google-services.json` is present. The included API provisioning function is a backend template; wiring its returned credentials into the Setup screen is the next deployment step because the actual Firebase project URL/region cannot be invented inside a portable source archive.

## Source tree

```text
app/src/main/java/com/irsyadlabs/espbridge/
├── collector/       Notification, navigation, location, phone state
├── core/            Models, permissions, Keystore security
├── data/            Auth, Firebase, DataStore, weather
├── service/         Background BLE connection services
├── state/           PhoneStateHub latest-state memory model
├── transport/       BLE + cloud routing
└── ui/              Theme, reusable components and screens
```

## What requires real hardware/configuration

The UI can be launched immediately. These features require external configuration:
- real Firebase email/Google authentication → your `google-services.json`
- Firebase RTDB writes → your Firebase project + rules
- API mode on ESP32 → deploy the included Cloud Functions template
- BLE data display → ESP32 firmware implementing `docs/BLE_PROTOCOL.md`
- real notification capture → physical Android device with Notification Access
- background BLE behavior → physical Android device; emulators are not representative for BLE
