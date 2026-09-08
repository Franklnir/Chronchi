package com.irsyadlabs.espbridge.core.model

enum class ConnectionMode {
    AUTOMATIC,
    BLUETOOTH_ONLY,
    CLOUD_ONLY
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    DISCOVERING,
    SYNCING,
    CONNECTED,
    ERROR
}
