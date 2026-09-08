# Firebase and Play Services include their own consumer rules.
# Keep NotificationListenerService and Android services referenced from the manifest.
-keep class com.irsyadlabs.espbridge.collector.NotificationBridgeService { *; }
-keep class com.irsyadlabs.espbridge.service.DeviceConnectionService { *; }
-keep class com.irsyadlabs.espbridge.service.EspCompanionDeviceService { *; }
