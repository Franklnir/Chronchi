package com.irsyadlabs.espbridge.collector

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.irsyadlabs.espbridge.EspBridgeApp
import com.irsyadlabs.espbridge.core.model.BleDeliveryState
import com.irsyadlabs.espbridge.core.model.NavigationDisplayState
import com.irsyadlabs.espbridge.core.model.SupportedSources
import com.irsyadlabs.espbridge.service.DeviceConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationBridgeService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var navigationMonitorJob: Job? = null
    private var currentNavigationKey: String? = null
    private var lastNavigationFingerprint: String? = null
    private var lastNavigationSendAt = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        startNavigationMonitor()
        scope.launch { ensureConnectionServiceRunning() }
    }

    override fun onListenerDisconnected() {
        navigationMonitorJob?.cancel()
        navigationMonitorJob = null
        scope.launch {
            delay(LISTENER_DISCONNECT_GRACE_MS)
            if (navigationMonitorJob == null) publishNavigationInactive()
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val container = EspBridgeApp.instance.container

        scope.launch {
            val settings = container.settings.settings.first()
            if (notification.packageName == GOOGLE_MAPS_PACKAGE && "navigation" in settings.selectedSourceIds) {
                reconcileMapsNavigation(notification)
                return@launch
            }

            val snapshot = notification.snapshot()
            val enabledSources = SupportedSources.allByPackage(notification.packageName)
                .filter { it.id in settings.selectedSourceIds }
            if (enabledSources.isEmpty()) return@launch

            val timestamp = notification.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
            val event = enabledSources.firstNotNullOfOrNull { source ->
                NotificationNormalizer.normalize(
                    source,
                    snapshot.title,
                    snapshot.primaryText,
                    snapshot.subText,
                    timestamp
                )
            } ?: return@launch

            container.stateHub.updateDisplayEvent(
                event,
                NotificationNormalizer.rawPreview(
                    snapshot.title,
                    snapshot.primaryText,
                    snapshot.allSupplemental.joinToString(" • ")
                )
            )
            container.router.sendNotification(event)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName != GOOGLE_MAPS_PACKAGE) return
        scope.launch {
            // Maps may replace one navigation notification with another. Give Android a
            // short moment to publish the replacement before deciding navigation stopped.
            delay(REMOVAL_RECONCILE_DELAY_MS)
            reconcileMapsNavigation()
        }
    }

    override fun onDestroy() {
        navigationMonitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startNavigationMonitor() {
        navigationMonitorJob?.cancel()
        navigationMonitorJob = scope.launch {
            while (isActive) {
                reconcileMapsNavigation()
                delay(NAVIGATION_POLL_MS)
            }
        }
    }

    private suspend fun reconcileMapsNavigation(preferred: StatusBarNotification? = null) {
        val container = EspBridgeApp.instance.container
        val settings = container.settings.settings.first()
        if ("navigation" !in settings.selectedSourceIds) {
            publishNavigationInactive()
            return
        }

        val candidates = runCatching { activeNotifications?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter(::isNavigationCandidate)
        val selected = preferred
            ?.takeIf(::isNavigationCandidate)
            ?: candidates.firstOrNull { it.key == currentNavigationKey }
            ?: candidates.maxByOrNull { it.postTime }

        if (selected == null) {
            publishNavigationInactive()
            return
        }

        val snapshot = selected.snapshot()
        val fingerprint = snapshot.fingerprint(selected.key)
        val now = System.currentTimeMillis()
        val current = container.stateHub.state.value.navigation
        val contentChanged = !current.active || fingerprint != lastNavigationFingerprint

        currentNavigationKey = selected.key
        if (contentChanged) {
            val parsed = NavigationParser.parse(
                title = snapshot.title,
                text = snapshot.primaryText,
                subText = snapshot.subText,
                timestamp = now,
                additionalText = snapshot.allSupplemental
            )
            lastNavigationFingerprint = fingerprint
            container.stateHub.updateNavigation(parsed)
            container.router.sendNavigation(parsed)
            lastNavigationSendAt = now
            return
        }

        val retryNeeded = current.bleDelivery == BleDeliveryState.PREVIEW_ONLY &&
            now - lastNavigationSendAt >= DELIVERY_RETRY_MS
        val heartbeatDue = current.bleDelivery == BleDeliveryState.SENT &&
            now - lastNavigationSendAt >= NAVIGATION_HEARTBEAT_MS
        if (retryNeeded || heartbeatDue) {
            container.router.sendNavigation(current)
            lastNavigationSendAt = now
        }
    }

    private fun isNavigationCandidate(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != GOOGLE_MAPS_PACKAGE) return false
        val snapshot = sbn.snapshot()
        val explicitNavigation = sbn.notification.category == Notification.CATEGORY_NAVIGATION
        return explicitNavigation ||
            (sbn.isOngoing && NavigationParser.looksLikeNavigation(
                snapshot.title,
                snapshot.primaryText,
                snapshot.subText,
                snapshot.allSupplemental
            ))
    }

    private fun publishNavigationInactive() {
        val container = EspBridgeApp.instance.container
        val current = container.stateHub.state.value.navigation
        val now = System.currentTimeMillis()
        currentNavigationKey = null
        lastNavigationFingerprint = null

        if (current.active) {
            val inactive = NavigationDisplayState(updatedAt = now)
            container.stateHub.updateNavigation(inactive)
            container.router.sendNavigation(inactive)
            lastNavigationSendAt = now
            return
        }

        // A failed stop packet remains the latest state and is retried until firmware ACKs it.
        if (current.updatedAt > 0L &&
            current.bleDelivery == BleDeliveryState.PREVIEW_ONLY &&
            now - lastNavigationSendAt >= DELIVERY_RETRY_MS
        ) {
            container.router.sendNavigation(current)
            lastNavigationSendAt = now
        }
    }

    private suspend fun ensureConnectionServiceRunning() {
        val settings = EspBridgeApp.instance.container.settings.settings.first()
        if (!settings.autoConnect || !settings.keepBackgroundConnection || settings.trustedDeviceAddress == null) return
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, DeviceConnectionService::class.java)
            )
        }
    }

    private fun StatusBarNotification.snapshot(): NotificationSnapshot {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val supplemental = buildList {
            extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.takeIf(String::isNotBlank)?.let(::add)
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.takeIf(String::isNotBlank)?.let(::add)
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map(CharSequence::toString)
                ?.filter(String::isNotBlank)
                ?.let(::addAll)
            text.takeIf { bigText.isNotBlank() && it.isNotBlank() }?.let(::add)
        }.distinct()
        return NotificationSnapshot(
            title = title,
            primaryText = bigText.ifBlank { text },
            subText = subText,
            allSupplemental = supplemental
        )
    }

    private data class NotificationSnapshot(
        val title: String,
        val primaryText: String,
        val subText: String,
        val allSupplemental: List<String>
    ) {
        fun fingerprint(key: String): String =
            listOf(key, title, primaryText, subText, allSupplemental.joinToString("|")).joinToString("\u0000")
    }

    companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
        private const val NAVIGATION_POLL_MS = 2_000L
        private const val NAVIGATION_HEARTBEAT_MS = 10_000L
        private const val DELIVERY_RETRY_MS = 3_000L
        private const val REMOVAL_RECONCILE_DELAY_MS = 300L
        private const val LISTENER_DISCONNECT_GRACE_MS = 3_000L
    }
}
