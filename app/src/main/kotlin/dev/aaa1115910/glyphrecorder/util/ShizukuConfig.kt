package dev.aaa1115910.glyphrecorder.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.IScreenCaptureService
import dev.aaa1115910.glyphrecorder.services.ScreenCaptureService
import io.github.oshai.kotlinlogging.KotlinLogging
import rikka.shizuku.Shizuku

object ShizukuConfig {
    private val logger = KotlinLogging.logger { }
    var screenCaptureService: IScreenCaptureService? = null
        private set
    var isShizukuConnected = false
        private set
    private val clientToken = Binder()
    private var connection: ServiceConnection? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ScreenCaptureService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    fun initShizuku(
        onServiceConnected: () -> Unit = {},
        onServiceDisconnected: () -> Unit = {}
    ) {
        if (isShizukuConnected && screenCaptureService?.asBinder()?.pingBinder() == true) {
            onServiceConnected()
            return
        }
        // Repeated taps while binding must not register multiple connections/start callbacks.
        if (connection != null) return
        val conn = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
                if (connection !== this) return
                runCatching {
                    check(binder.pingBinder()) { "Capture service is dead" }
                    val service = IScreenCaptureService.Stub.asInterface(binder)
                    service.attachClient(clientToken)
                    screenCaptureService = service
                    isShizukuConnected = true
                    onServiceConnected()
                }.onFailure {
                    logger.error(it) { "Unable to initialize capture service" }
                    stopShizuku()
                    onServiceDisconnected()
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                if (connection !== this) return
                screenCaptureService = null
                isShizukuConnected = false
                connection = null
                onServiceDisconnected()
            }
        }
        connection = conn
        runCatching {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(0)
                connection = null
                return
            }
            Shizuku.bindUserService(userServiceArgs, conn)
        }.onFailure {
            logger.error(it) { "Unable to bind capture service" }
            stopShizuku()
            onServiceDisconnected()
        }
    }

    fun stopShizuku() {
        val service = screenCaptureService
        val conn = connection
        screenCaptureService = null
        isShizukuConnected = false
        connection = null
        runCatching { service?.stopScreenCapture() }
            .onFailure { logger.warn(it) { "Unable to stop capture" } }
        if (conn != null || service != null) {
            // remove=true invokes the reserved destroy() transaction in the root process.
            runCatching { Shizuku.unbindUserService(userServiceArgs, conn, true) }
                .onFailure {
                    logger.warn(it) { "Unable to remove capture user service" }
                    runCatching { service?.destroy() }
                }
        }
    }
}
