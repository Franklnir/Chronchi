# Architecture

```text
Android data sources
├── NotificationListenerService
├── Google Maps notification parser
├── Fused Location
├── Weather repository
└── battery/network collector
          │
          ▼
 NotificationNormalizer
 compact DisplayEvent
          │
          ▼
      PhoneStateHub
      latest state RAM
       /       |          \
Live Monitor  OLED Preview  TransportRouter
                  /          \
                 /            \
            BLE GATT        Firebase RTDB
            offline-first    latest-state only
                 │                │
                 ▼                ▼
               ESP32          optional device API
                 │
                 ▼
                OLED
```

`TransportRouter` prevents source collectors from depending directly on BLE/Firebase implementation details. This allows MQTT/WebSocket/USB transports to be added later without rewriting notification/location collectors.

`NotificationNormalizer` limits message previews and maps supported notifications into one compact `DisplayEvent`. The Live Data Monitor, category-based OLED preview, BLE, and Firebase all consume that same in-memory object; there is no second parser or persistent history.

OLED preview selection is current-state only: active navigation is persistent and highest priority, recent notifications use short category timeouts, and the preview then returns to `HomeDisplayState`. Android sends normalized text and enum values, never bitmaps or OLED coordinates.
