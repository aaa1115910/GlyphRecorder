package dev.aaa1115910.glyphrecorder.util

import android.os.Handler
import android.os.Looper
import dev.aaa1115910.glyphrecorder.App

object SmartDisplayCoordinator {
    private const val INGRESS_PACKAGE_NAME = "com.nianticproject.ingress"
    private const val SYSTEM_UI_PACKAGE_NAME = "com.android.systemui"
    private const val CLOSE_DEBOUNCE_MS = 3_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var latestEventPackage: String? = null

    @Volatile
    private var latestNonIgnoredPackage: String? = null

    @Volatile
    private var activeWindowPackage: String? = null

    @Volatile
    private var activeWindowPackageProvider: (() -> String?)? = null

    @Volatile
    private var accessibilityServiceConnected = false

    private var state = OverlayVisibilityState.HIDDEN

    private val closeRunnable = Runnable {
        // Double-check foreground package right before hiding.
        val packageName = resolveEffectiveForegroundPackage(currentForegroundPackageForCloseCheck())
        if (isTargetIngressPackage(packageName)) {
            if (Prefs.working && Prefs.smartDisplayEnabled) {
                state = OverlayVisibilityState.SHOWING
                FloatingWindowController.show(App.context)
            } else {
                state = OverlayVisibilityState.HIDDEN
            }
            return@Runnable
        }

        FloatingWindowController.hide(App.context)
        state = OverlayVisibilityState.HIDDEN
    }

    fun onAppStarted() {
        onWorkingModeChanged(Prefs.working, Prefs.smartDisplayEnabled)
    }

    fun onForegroundPackageChanged(packageName: String?) {
        runOnMain {
            latestEventPackage = normalizePackage(packageName)
            if (!isIgnoredPackage(latestEventPackage)) {
                latestNonIgnoredPackage = latestEventPackage
            }
            syncByForegroundPackage(currentForegroundPackageForCloseCheck())
        }
    }

    fun onActiveWindowPackageChanged(packageName: String?) {
        activeWindowPackage = normalizePackage(packageName)
    }

    fun setActiveWindowPackageProvider(provider: (() -> String?)?) {
        activeWindowPackageProvider = provider
    }

    fun onWorkingModeChanged(working: Boolean, smartDisplayEnabled: Boolean) {
        runOnMain {
            applyMode(working, smartDisplayEnabled)
        }
    }

    fun setAccessibilityServiceConnected(connected: Boolean) {
        accessibilityServiceConnected = connected
    }

    fun onManualClose() {
        runOnMain {
            cancelPendingClose()
            FloatingWindowController.hide(App.context)
            state = OverlayVisibilityState.HIDDEN
            latestEventPackage = null
            latestNonIgnoredPackage = null
            activeWindowPackage = null
        }
    }

    fun isCurrentDetectorAvailable(): Boolean {
        return accessibilityServiceConnected ||
                DetectorPermissionUtil.isAccessibilityServiceEnabled(App.context)
    }

    private fun applyMode(working: Boolean, smartDisplayEnabled: Boolean) {
        if (!working) {
            cancelPendingClose()
            FloatingWindowController.hide(App.context)
            state = OverlayVisibilityState.HIDDEN
            latestEventPackage = null
            latestNonIgnoredPackage = null
            activeWindowPackage = null
            return
        }

        if (!smartDisplayEnabled) {
            cancelPendingClose()
            FloatingWindowController.show(App.context)
            state = OverlayVisibilityState.SHOWING
            return
        }

        if (!isCurrentDetectorAvailable()) {
            cancelPendingClose()
            FloatingWindowController.hide(App.context)
            state = OverlayVisibilityState.HIDDEN
            return
        }

        syncByForegroundPackage(currentForegroundPackageForCloseCheck())
    }

    private fun syncByForegroundPackage(packageName: String?) {
        if (!Prefs.working) {
            applyMode(working = false, smartDisplayEnabled = Prefs.smartDisplayEnabled)
            return
        }

        if (!Prefs.smartDisplayEnabled) {
            applyMode(working = true, smartDisplayEnabled = false)
            return
        }

        if (!isCurrentDetectorAvailable()) {
            applyMode(working = true, smartDisplayEnabled = true)
            return
        }

        if (isIgnoredPackage(packageName)) {
            // Keep previous state, do not schedule close.
            return
        }

        if (isTargetIngressPackage(packageName)) {
            cancelPendingClose()
            FloatingWindowController.show(App.context)
            state = OverlayVisibilityState.SHOWING
            return
        }

        scheduleClose()
    }

    private fun scheduleClose() {
        if (state == OverlayVisibilityState.PENDING_CLOSE) return
        cancelPendingClose()
        state = OverlayVisibilityState.PENDING_CLOSE
        mainHandler.postDelayed(closeRunnable, CLOSE_DEBOUNCE_MS)
    }

    private fun cancelPendingClose() {
        mainHandler.removeCallbacks(closeRunnable)
    }

    private fun currentForegroundPackageForCloseCheck(): String? {
        val packageFromProvider = runCatching {
            activeWindowPackageProvider?.invoke()
        }.getOrNull().let(::normalizePackage)

        if (!packageFromProvider.isNullOrEmpty()) return packageFromProvider
        if (!activeWindowPackage.isNullOrEmpty()) return activeWindowPackage
        return latestEventPackage
    }

    private fun resolveEffectiveForegroundPackage(packageName: String?): String? {
        val normalized = normalizePackage(packageName)
        if (normalized.isNullOrEmpty()) return latestNonIgnoredPackage
        if (isIgnoredPackage(normalized)) return latestNonIgnoredPackage
        return normalized
    }

    private fun isTargetIngressPackage(packageName: String?): Boolean {
        return normalizePackage(packageName) == INGRESS_PACKAGE_NAME
    }

    private fun isIgnoredPackage(packageName: String?): Boolean {
        val pkg = normalizePackage(packageName) ?: return false
        if (pkg == App.context.packageName) return true
        if (pkg == SYSTEM_UI_PACKAGE_NAME) return true
        if (pkg.contains("permissioncontroller", ignoreCase = true)) return true

        val imePackage = DetectorPermissionUtil.currentInputMethodPackage(App.context)
        if (!imePackage.isNullOrEmpty() && pkg == imePackage) return true

        return false
    }

    private fun normalizePackage(packageName: String?): String? {
        return packageName?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
