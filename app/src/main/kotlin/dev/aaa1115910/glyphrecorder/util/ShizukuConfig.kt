package dev.aaa1115910.glyphrecorder.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.IScreenCaptureService
import dev.aaa1115910.glyphrecorder.services.ScreenCaptureService
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
            isShizukuConnected = false
            onServiceDisconnected()
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
}