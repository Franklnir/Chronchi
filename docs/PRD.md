# PRD — ESP Bridge Android Companion

## Goal
Build an Android-only companion application that forwards selected phone information to an ESP32 display using Bluetooth LE while remaining useful without internet. Firebase adds authentication and an optional cloud/API transport but is not required for local operation.

## Core requirements
- Kotlin + Jetpack Compose.
- Email/password login and registration; confirm password is local validation only.
- Google Sign-In when Firebase is configured.
- Persistent Firebase data is limited to account/device metadata necessary for identity/API; transient phone data uses fixed latest-state paths.
- No notification, location, navigation, battery or signal history.
- Bluetooth transport works offline.
- Trusted ESP32 auto-reconnects after initial user-approved pairing/association.
- Notification forwarding is opt-in per app/source.
- Background operation follows Android foreground/companion-device rules and never attempts to bypass Force Stop.

## Primary navigation
1. Home — status preview and source toggles.
2. Setup — BLE pairing, connection mode, cloud/API credentials.
3. Settings — profile, permissions, background behavior, privacy.

Authentication and permission onboarding are outside the bottom navigation.

## Source categories
### Messaging & Social
WhatsApp, WhatsApp Business, Telegram, Instagram, Facebook, Messenger, TikTok, X, Threads.

### Professional
Gmail, LinkedIn, JobStreet.

### Payments/finance
DANA, OVO, GoPay/Gojek, ShopeePay and supported banking apps.

### Shopping/services
Shopee, Shopee Partner, Tokopedia, Lazada, Gojek, Grab.

### System
Navigation, Weather, Location, Phone Status, Network Status.

## Offline behavior
- Notification Listener → StateHub → BLE → ESP32 works without network.
- GPS can work offline at the Android location-provider level.
- Google Maps guidance depends on what Google Maps itself can provide in the user's current offline/online navigation context.
- Weather cannot refresh without network; cached weather is explicitly marked cached/stale.
- Firebase/API is disabled while validated internet is unavailable.

## OLED data priority recommendation
1. urgent navigation maneuver
2. incoming communication/call state if later implemented
3. transaction notification
4. message notification
5. other notification
6. default clock/date/weather/status screen

Battery/signal updates should update status icons without interrupting higher-priority content.

## Non-goals
- reading WhatsApp/DANA/private app databases
- SMS/call-log/contact harvesting
- storing message history
- accessibility-based scraping
- continuous broad app/package surveillance
