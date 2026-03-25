package dev.aaa1115910.glyphrecorder.services

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.net.toUri
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.activities.MainActivity
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.ShizukuConfig
import dev.aaa1115910.glyphrecorder.util.SmartDisplayCoordinator
import dev.aaa1115910.glyphrecorder.util.WorkingSessionController
import io.github.oshai.kotlinlogging.KotlinLogging

class WorkingQuickSettingsTileService : TileService() {
    companion object {
        private val logger = KotlinLogging.logger { }

        fun requestStateSync(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context, WorkingQuickSettingsTileService::class.java)
                )
            }.onFailure {
                logger.warn(it) { "Failed to request quick settings tile refresh" }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (Prefs.working) {
            stopWorking()
        } else {
            startWorking()
        }
        refreshTile()
    }

    private fun startWorking() {
        if (!Settings.canDrawOverlays(this)) {
            showToast(getString(R.string.quick_tile_need_overlay_permission))
            openOverlayPermissionSettings()
            return
        }

        if (Prefs.circles.size != 11) {
            showToast(getString(R.string.start_service_failed_need_correction_position))
            openMainActivity()
            return
        }

        when (Prefs.workingMode) {
            WorkingMode.MediaProjection -> startMediaProjectionMode()
            WorkingMode.Shizuku -> startShizukuMode()
        }
    }

    private fun startMediaProjectionMode() {
        val resultData = MediaProjectionService.resultData
        val resultCode = MediaProjectionService.resultCode

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            startForegroundService(Intent(this, MediaProjectionService::class.java))
            setWorking(true)
            return
        }

        setWorking(false)
        showToast(getString(R.string.quick_tile_media_projection_need_app_start))
        openMainActivity()
    }

    private fun startShizukuMode() {
        val launchService: () -> Unit = launch@{
            val service = ShizukuConfig.screenCaptureService
            if (service == null) {
                showToast(getString(R.string.start_service_failed_shizuku))
                setWorking(false)
                return@launch
            }

            val circlesRawString = Prefs.circles.joinToString(",") { "${it.first}:${it.second}" }
            val startResult = runCatching {
                service.updateCircles(circlesRawString)
                service.startScreenCapture()
            }.onFailure {
                logger.error(it) { "Failed to start screen capture from quick tile" }
            }.getOrDefault(false)

            if (startResult) {
                setWorking(true)
            } else {
                showToast(getString(R.string.start_service_failed_shizuku))
                setWorking(false)
            }
        }

        if (ShizukuConfig.isShizukuConnected && ShizukuConfig.screenCaptureService != null) {
            launchService()
        } else {
            ShizukuConfig.initShizuku(onServiceConnected = {
                launchService()
            })
            showToast(getString(R.string.quick_tile_connecting_shizuku))
        }
    }

    private fun stopWorking() {
        setWorking(false)
        WorkingSessionController.stopCurrentSession(this)
    }

    private fun setWorking(working: Boolean) {
        Prefs.working = working
        SmartDisplayCoordinator.onWorkingModeChanged(
            working = Prefs.working,
            smartDisplayEnabled = Prefs.smartDisplayEnabled
        )
        requestStateSync(this)
        refreshTile()
    }

    private fun refreshTile() {
        qsTile?.let { tile ->
            tile.label = getString(R.string.quick_tile_working_label)
            tile.subtitle = if (Prefs.working) {
                getString(R.string.home_screen_stop_service)
            } else {
                getString(R.string.home_screen_start_service)
            }
            tile.state = if (Prefs.working) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivityAndCollapseCompat(intent)
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapseCompat(intent)
    }

    private fun startActivityAndCollapseCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
