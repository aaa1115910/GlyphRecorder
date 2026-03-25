package dev.aaa1115910.glyphrecorder.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.aaa1115910.glyphrecorder.App
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.IScreenCaptureService
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.services.ScreenCaptureService
import dev.aaa1115910.glyphrecorder.services.WorkingQuickSettingsTileService
import io.github.oshai.kotlinlogging.KotlinLogging
import rikka.shizuku.Shizuku

object ShizukuConfig {
    private val logger = KotlinLogging.logger { }
    var screenCaptureService: IScreenCaptureService? = null
    var isShizukuConnected = false

    private fun createUserServiceConnection(
        onServiceConnected: () -> Unit,
        onServiceDisconnected: () -> Unit
    ) = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            if (binder.pingBinder()) {
                screenCaptureService = IScreenCaptureService.Stub.asInterface(binder)
                isShizukuConnected = true
                logger.info { "Shizuku connected" }
                onServiceConnected()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            screenCaptureService = null
            isShizukuConnected = false
            onServiceDisconnected()
            if (Prefs.working && Prefs.workingMode == WorkingMode.Shizuku) {
                Prefs.working = false
                WorkingQuickSettingsTileService.requestStateSync(App.context)
                SmartDisplayCoordinator.onWorkingModeChanged(
                    working = false,
                    smartDisplayEnabled = Prefs.smartDisplayEnabled
                )
            }
            logger.info { "Shizuku disconnected" }
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ScreenCaptureService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(1)

    fun initShizuku(
        onServiceConnected: () -> Unit = {},
        onServiceDisconnected: () -> Unit = {}
    ) {
        println("Init shizuku")
        runCatching {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(0)
            }
            Shizuku.bindUserService(
                userServiceArgs,
                createUserServiceConnection(onServiceConnected, onServiceDisconnected)
            )
        }
    }

    fun stopScreenCapture(): Boolean {
        val service = screenCaptureService ?: return false
        return runCatching {
            if (!service.asBinder().pingBinder()) return@runCatching false
            service.stopScreenCapture()
            true
        }.onFailure {
            logger.error(it) { "Failed to stop Shizuku screen capture" }
        }.getOrDefault(false)
    }

    fun isScreenCaptureRunning(): Boolean {
        val service = screenCaptureService ?: return false
        return runCatching {
            service.asBinder().pingBinder() && service.isScreenCaptureRunning()
        }.onFailure {
            logger.warn(it) { "Failed to query Shizuku screen capture state" }
        }.getOrDefault(false)
    }
}
