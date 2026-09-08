# Data model

## Local Android storage
DataStore persists only preferences/configuration:
- selected notification sources
- connection mode
- auto-connect setting
- trusted ESP address/name
- onboarding state
- cached weather

Android Keystore encrypts API/device secrets before ciphertext is stored locally.

Transient data stays in `PhoneStateHub` RAM:
- latest normalized display event and optional raw monitor preview
- Home and category-based OLED preview state
- navigation state
- location state
- battery/network status
- current weather

No notification/GPS/navigation history is intentionally created.
The raw monitor preview is never written to Firebase and is replaced in RAM by the next supported event.

## Realtime Database
```text
/users/{uid}/profile
/devices/{deviceId}
/live/{uid}/{deviceId}/phone
/live/{uid}/{deviceId}/network
/live/{uid}/{deviceId}/weather
/live/{uid}/{deviceId}/location
/live/{uid}/{deviceId}/navigation
/live/{uid}/{deviceId}/notification
/device_api/{deviceId}   # Admin Functions only
```

Every `/live/...` write uses `setValue()` on a fixed path. It overwrites the previous state rather than using `push()`.

The notification value is compact and normalized:

```text
notification/
  sourceApp
  category
  primaryText
  secondaryText   # optional
  tertiaryText    # optional
  timestamp
```

The profile path contains only `displayName`, `email`, `authProvider`, `createdAt`, and `updatedAt`. Avatars are derived locally from initials; Firebase Storage and profile image fields are not used.

When internet is unavailable, `FirebaseCloudTransport.available()` is false and the app does not issue the cloud write. BLE remains independent.
