package com.irsyadlabs.espbridge.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class OledTemplate {
    HOME,
    MESSAGE,
    PROFESSIONAL,
    PAYMENT,
    ORDER,
    NAVIGATION,
    SYSTEM
}

data class OledPreviewState(
    val template: OledTemplate,
    val home: HomeDisplayState,
    val event: DisplayEvent? = null,
    val navigation: NavigationDisplayState? = null
)

object OledPreviewSelector {
    const val MESSAGE_TIMEOUT_MS = 5_000L
    const val PROFESSIONAL_TIMEOUT_MS = 5_000L
    const val PAYMENT_TIMEOUT_MS = 6_000L
    const val ORDER_TIMEOUT_MS = 5_000L
    const val SYSTEM_TIMEOUT_MS = 3_000L

    fun select(state: PhoneState, nowMillis: Long = System.currentTimeMillis()): OledPreviewState {
        val home = state.homeDisplay.withTime(nowMillis)
        if (state.navigation.active) {
            return OledPreviewState(
                template = OledTemplate.NAVIGATION,
                home = home,
                navigation = state.navigation
            )
        }

        val event = state.liveData.event
        val eventAge = event?.let { (nowMillis - it.timestamp).coerceAtLeast(0L) }
        if (event != null && eventAge != null && eventAge <= timeoutFor(event.category)) {
            return OledPreviewState(
                template = event.category.toOledTemplate(),
                home = home,
                event = event
            )
        }
        return OledPreviewState(template = OledTemplate.HOME, home = home)
    }

    fun timeoutFor(category: DisplayCategory): Long = when (category) {
        DisplayCategory.PAYMENT -> PAYMENT_TIMEOUT_MS
        DisplayCategory.MESSAGE -> MESSAGE_TIMEOUT_MS
        DisplayCategory.PROFESSIONAL -> PROFESSIONAL_TIMEOUT_MS
        DisplayCategory.ORDER -> ORDER_TIMEOUT_MS
        DisplayCategory.SYSTEM -> SYSTEM_TIMEOUT_MS
        DisplayCategory.NAVIGATION -> Long.MAX_VALUE
    }

    fun priorityFor(template: OledTemplate): Int = when (template) {
        OledTemplate.NAVIGATION -> 100
        OledTemplate.PAYMENT -> 80
        OledTemplate.MESSAGE, OledTemplate.PROFESSIONAL -> 70
        OledTemplate.ORDER -> 60
        OledTemplate.SYSTEM -> 50
        OledTemplate.HOME -> 0
    }

    private fun DisplayCategory.toOledTemplate(): OledTemplate = when (this) {
        DisplayCategory.MESSAGE -> OledTemplate.MESSAGE
        DisplayCategory.PROFESSIONAL -> OledTemplate.PROFESSIONAL
        DisplayCategory.PAYMENT -> OledTemplate.PAYMENT
        DisplayCategory.ORDER -> OledTemplate.ORDER
        DisplayCategory.NAVIGATION -> OledTemplate.NAVIGATION
        DisplayCategory.SYSTEM -> OledTemplate.SYSTEM
    }

    private fun HomeDisplayState.withTime(nowMillis: Long): HomeDisplayState {
        val dateTime = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        return copy(
            time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            date = dateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID")))
        )
    }
}
