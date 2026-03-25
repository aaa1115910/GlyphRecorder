package dev.aaa1115910.glyphrecorder.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import dev.aaa1115910.glyphrecorder.util.SmartDisplayCoordinator
import dev.aaa1115910.glyphrecorder.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging

class IngressAccessibilityService : AccessibilityService() {
    private val logger = KotlinLogging.logger { }

    override fun onServiceConnected() {
        super.onServiceConnected()
        logger.info { "IngressAccessibilityService connected" }
        SmartDisplayCoordinator.setAccessibilityServiceConnected(true)
        val activePackage = rootInActiveWindow?.packageName?.toString()
        SmartDisplayCoordinator.setActiveWindowPackageProvider {
            rootInActiveWindow?.packageName?.toString()
        }
        SmartDisplayCoordinator.onActiveWindowPackageChanged(activePackage)
        SmartDisplayCoordinator.onForegroundPackageChanged(activePackage)
        SmartDisplayCoordinator.onWorkingModeChanged(
            working = Prefs.working,
            smartDisplayEnabled = Prefs.smartDisplayEnabled
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        val activePackage = rootInActiveWindow?.packageName?.toString()
        val eventPackage = event.packageName?.toString()
        val effectivePackage = activePackage ?: eventPackage
        SmartDisplayCoordinator.onActiveWindowPackageChanged(activePackage)
        SmartDisplayCoordinator.onForegroundPackageChanged(effectivePackage)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        SmartDisplayCoordinator.setAccessibilityServiceConnected(false)
        SmartDisplayCoordinator.setActiveWindowPackageProvider(null)
        SmartDisplayCoordinator.onActiveWindowPackageChanged(null)
        SmartDisplayCoordinator.onForegroundPackageChanged(null)
        logger.info { "IngressAccessibilityService destroyed" }
        super.onDestroy()
    }
}
