package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import android.content.Intent
import dev.aaa1115910.glyphrecorder.App
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.services.MediaProjectionService
import dev.aaa1115910.glyphrecorder.services.WorkingQuickSettingsTileService
import io.github.oshai.kotlinlogging.KotlinLogging

object WorkingSessionController {
    private val logger = KotlinLogging.logger { }

    fun reconcilePersistedWorkingState() {
        if (!Prefs.working) return
        if (isCurrentSessionAlive()) return

        logger.info { "Clearing stale working state, mode=${Prefs.workingMode}" }
        Prefs.working = false
        WorkingQuickSettingsTileService.requestStateSync(App.context)
    }

    fun stopCurrentSession(context: Context) {
        when (Prefs.workingMode) {
            WorkingMode.MediaProjection -> {
                context.stopService(Intent(context, MediaProjectionService::class.java))
            }

            WorkingMode.Shizuku -> {
                ShizukuConfig.stopScreenCapture()
            }
        }
    }

    fun isCurrentSessionAlive(): Boolean {
        return when (Prefs.workingMode) {
            WorkingMode.MediaProjection -> MediaProjectionService.running
            WorkingMode.Shizuku -> ShizukuConfig.isScreenCaptureRunning()
        }
    }
}
