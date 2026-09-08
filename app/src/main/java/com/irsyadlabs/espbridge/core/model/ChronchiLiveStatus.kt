package com.irsyadlabs.espbridge.core.model

data class ChronchiLiveStatus(
    val title: String,
    val primary: String,
    val secondary: String = "",
    val footer: String = ""
) {
    val expandedText: String
        get() = listOf(primary, secondary, footer).filter(String::isNotBlank).joinToString("\n")
}

object ChronchiLiveStatusFormatter {
    fun format(state: PhoneState, nowMillis: Long = System.currentTimeMillis()): ChronchiLiveStatus {
        val preview = OledPreviewSelector.select(state, nowMillis)
        val event = preview.event
        return when (preview.template) {
            OledTemplate.NAVIGATION -> {
                val navigation = preview.navigation ?: state.navigation
                ChronchiLiveStatus(
                    title = "CHRONCHI • NAVIGASI",
                    primary = listOf(navigation.distanceText, maneuverText(navigation.maneuver))
                        .filter(String::isNotBlank)
                        .joinToString(" "),
                    secondary = navigation.roadName,
                    footer = navigation.destinationDistanceText.orEmpty()
                )
            }
            OledTemplate.HOME -> ChronchiLiveStatus(
                title = "CHRONCHI • HOME",
                primary = listOf(preview.home.time, preview.home.date).filter(String::isNotBlank).joinToString(" • "),
                secondary = listOfNotNull(preview.home.temperature, preview.home.locationName)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                footer = "Baterai ${preview.home.batteryLevel}%"
            )
            OledTemplate.PAYMENT -> ChronchiLiveStatus(
                title = "CHRONCHI • ${event?.sourceApp.orEmpty()}",
                primary = event?.primaryText.orEmpty(),
                secondary = event?.secondaryText.orEmpty(),
                footer = event?.tertiaryText.orEmpty()
            )
            OledTemplate.MESSAGE,
            OledTemplate.PROFESSIONAL,
            OledTemplate.ORDER,
            OledTemplate.SYSTEM -> ChronchiLiveStatus(
                title = "CHRONCHI • ${event?.sourceApp.orEmpty()}",
                primary = event?.primaryText.orEmpty(),
                secondary = event?.secondaryText.orEmpty(),
                footer = event?.tertiaryText.orEmpty()
            )
        }
    }

    private fun maneuverText(maneuver: Maneuver): String = when (maneuver) {
        Maneuver.STRAIGHT -> "lurus"
        Maneuver.LEFT -> "belok kiri"
        Maneuver.RIGHT -> "belok kanan"
        Maneuver.SLIGHT_LEFT -> "serong kiri"
        Maneuver.SLIGHT_RIGHT -> "serong kanan"
        Maneuver.ROUNDABOUT -> "bundaran"
        Maneuver.ARRIVE -> "sampai"
        Maneuver.UNKNOWN -> "lanjutkan"
    }
}
