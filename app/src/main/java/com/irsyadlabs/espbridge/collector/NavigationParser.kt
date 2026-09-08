package com.irsyadlabs.espbridge.collector

import com.irsyadlabs.espbridge.core.model.Maneuver
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NavigationParser {
    private val distanceRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(km|m)", RegexOption.IGNORE_CASE)
    private val roadRegex = Regex(
        "(?:ke|menuju|onto|on|toward|via)\\s+([^,;•|]+)",
        RegexOption.IGNORE_CASE
    )
    private val navigationWords = listOf(
        "belok", "putar balik", "bundaran", "lurus", "serong", "ambil kiri", "ambil kanan",
        "turn", "u-turn", "roundabout", "continue", "keep left", "keep right", "slight",
        "keluar", "exit", "menuju", "toward", "tiba", "sampai", "arrive", "destination"
    )

    fun parse(
        title: String,
        text: String,
        subText: String = "",
        timestamp: Long = System.currentTimeMillis(),
        additionalText: List<String> = emptyList()
    ): NavigationDisplayState {
        val cleanTitle = NotificationNormalizer.sanitizeText(title, 40)
        val cleanText = NotificationNormalizer.sanitizeText(text, 100)
        val cleanSubText = NotificationNormalizer.sanitizeText(subText, 60)
        val cleanAdditional = additionalText.map { NotificationNormalizer.sanitizeText(it, 100) }
        val combined = (listOf(cleanTitle, cleanText, cleanSubText) + cleanAdditional)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" • ")
        val lower = combined.lowercase(Locale.ROOT)
        val maneuver = when {
            containsAny(lower, "bundaran", "roundabout") -> Maneuver.ROUNDABOUT
            containsAny(lower, "sedikit ke kanan", "serong kanan", "slight right") -> Maneuver.SLIGHT_RIGHT
            containsAny(lower, "sedikit ke kiri", "serong kiri", "slight left") -> Maneuver.SLIGHT_LEFT
            containsAny(lower, "belok kanan", "turn right", "keep right", "ambil kanan") -> Maneuver.RIGHT
            containsAny(lower, "belok kiri", "turn left", "keep left", "ambil kiri") -> Maneuver.LEFT
            containsAny(lower, "tiba", "sampai", "destination", "arrive") -> Maneuver.ARRIVE
            containsAny(lower, "lurus", "straight", "continue") -> Maneuver.STRAIGHT
            else -> Maneuver.UNKNOWN
        }

        val distanceMatches = distanceRegex.findAll(combined).toList()
        val nextDistance = distanceMatches.firstOrNull()
        val numeric = nextDistance?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        val unit = nextDistance?.groupValues?.getOrNull(2)?.lowercase(Locale.ROOT)
        val distanceMeters = when (unit) {
            "km" -> numeric?.times(1000)?.toInt()
            "m" -> numeric?.toInt()
            else -> null
        }
        val destinationDistance = distanceMatches.getOrNull(1)?.value?.let { "Tujuan $it" }
        val road = roadRegex.find(combined)?.groupValues?.getOrNull(1)
            ?.trim(' ', '-', '.')
            ?.let { NotificationNormalizer.sanitizeText(it, 32) }
            .orEmpty()
            .ifBlank {
                cleanSubText.takeIf(String::isNotBlank)
                    ?: cleanTitle.takeUnless { it.equals("Google Maps", ignoreCase = true) }.orEmpty()
            }

        return NavigationDisplayState(
            active = true,
            maneuver = maneuver,
            distanceMeters = distanceMeters,
            distanceText = nextDistance?.value?.uppercase(Locale.ROOT).orEmpty(),
            roadName = road,
            destinationDistanceText = destinationDistance,
            time = formatTime(timestamp),
            updatedAt = timestamp
        )
    }

    fun looksLikeNavigation(
        title: String,
        text: String,
        subText: String = "",
        additionalText: List<String> = emptyList()
    ): Boolean {
        val combined = (listOf(title, text, subText) + additionalText)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        return distanceRegex.containsMatchIn(combined) || navigationWords.any(combined::contains)
    }

    private fun formatTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun containsAny(source: String, vararg needles: String): Boolean = needles.any(source::contains)
}
