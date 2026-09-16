package com.irsyadlabs.espbridge.core.model

data class XichiLiveStatus(
    val title: String,
    val primary: String,
    val secondary: String = "",
    val footer: String = ""
) {
    val expandedText: String
        get() = listOf(primary, secondary, footer).filter(String::isNotBlank).joinToString("\n")
}

object XichiLiveStatusFormatter {
    fun format(state: PhoneState, nowMillis: Long = System.currentTimeMillis()): XichiLiveStatus {
        val preview = OledPreviewSelector.select(state, nowMillis)
        val event = preview.event
        return when (preview.template) {
            OledTemplate.NAVIGATION -> {
                val navigation = preview.navigation ?: state.navigation
                XichiLiveStatus(
                    title = "XICHI • NAVIGASI",
                    primary = listOf(navigation.distanceText, maneuverText(navigation.maneuver))
                        .filter(String::isNotBlank)
                        .joinToString(" "),
                    secondary = navigation.roadName,
                    footer = navigation.destinationDistanceText.orEmpty()
                )
            }
            OledTemplate.HOME -> XichiLiveStatus(
                title = "XICHI • HOME",
                primary = listOf(preview.home.time, preview.home.date).filter(String::isNotBlank).joinToString(" • "),
                secondary = listOfNotNull(preview.home.temperature, preview.home.locationName)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                footer = "Baterai ${preview.home.batteryLevel}%"
            )
            OledTemplate.PAYMENT -> XichiLiveStatus(
                title = "XICHI • ${event?.sourceApp.orEmpty()}",
                primary = event?.primaryText.orEmpty(),
                secondary = event?.secondaryText.orEmpty(),
                footer = event?.tertiaryText.orEmpty()
            )
            OledTemplate.MESSAGE,
            OledTemplate.PROFESSIONAL,
            OledTemplate.ORDER,
            OledTemplate.SYSTEM -> XichiLiveStatus(
                title = "XICHI • ${event?.sourceApp.orEmpty()}",
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
