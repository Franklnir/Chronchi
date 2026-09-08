package com.irsyadlabs.espbridge.core.model

enum class DisplayCategory(val label: String) {
    MESSAGE("Message"),
    PROFESSIONAL("Professional"),
    PAYMENT("Payment"),
    ORDER("Order"),
    NAVIGATION("Navigation"),
    SYSTEM("System")
}

enum class PaymentDirection(val label: String, val symbol: String) {
    INCOMING("Incoming", "+"),
    OUTGOING("Outgoing", "-"),
    UNKNOWN("Unknown", "")
}

enum class OrderStatus(val label: String) {
    CONFIRMED("Pesanan Dikonfirmasi"),
    PACKED("Sedang Dikemas"),
    SHIPPED("Pesanan Dikirim"),
    IN_TRANSIT("Dalam Perjalanan"),
    OUT_FOR_DELIVERY("Sedang Diantar"),
    DELIVERED("Pesanan Diterima"),
    CANCELLED("Pesanan Dibatalkan"),
    UNKNOWN("Status Pesanan")
}

data class DisplayEvent(
    val category: DisplayCategory,
    val sourceApp: String,
    val primaryText: String,
    val secondaryText: String? = null,
    val tertiaryText: String? = null,
    val timestamp: Long,
    val paymentDirection: PaymentDirection? = null,
    val orderStatus: OrderStatus? = null
) {
    fun espDisplayLines(): List<String> = buildList {
        add(sourceApp)
        add(primaryText)
        secondaryText?.takeIf { it.isNotBlank() }?.let(::add)
        tertiaryText?.takeIf { it.isNotBlank() }?.let(::add)
    }
}

enum class BleDeliveryState {
    PREVIEW_ONLY,
    SENDING,
    SENT
}

data class LiveDataState(
    val event: DisplayEvent? = null,
    val rawPreview: String = "",
    val bleDelivery: BleDeliveryState = BleDeliveryState.PREVIEW_ONLY
)
