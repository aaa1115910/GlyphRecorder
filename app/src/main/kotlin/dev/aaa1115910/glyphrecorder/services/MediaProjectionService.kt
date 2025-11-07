package dev.aaa1115910.glyphrecorder.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.hardware.display.DisplayManagerCompat
import dev.aaa1115910.glyphrecorder.App
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.toBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class MediaProjectionService : Service() {
    companion object {
        private var mMediaProjection: MediaProjection? = null
        private var mImageReader: ImageReader? = null
        private var mVirtualDisplayImageReader: VirtualDisplay? = null
        var resultCode: Int = 0
        var resultData: Intent? = null
        private var mImageAvailable = false
        var running: Boolean = false

        private const val NOTIFICATION_CHANNEL_ID = "MediaProjectionChannelId"
        private const val NOTIFICATION_CHANNEL_NAME = "MediaProjection"
        private const val NOTIFICATION_ID = 1

        private val MEDIA_PROJECTION_CALLBACK: MediaProjection.Callback =
            object : MediaProjection.Callback() {
                override fun onStop() {
                    logger.info { "Media projection stopped by system" }
                    Prefs.working = false
                }
            }

        private val logger = KotlinLogging.logger { }
    }

    private val mBinder = MediaProjectionServiceBinder()
    private var lastBitmap: Bitmap? = null

    override fun onCreate() {
        logger.info { "on start service" }
        super.onCreate()
        startNotification()
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
        createImageReaderVirtualDisplay()
        running = true
    }

    private fun startNotification() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("")
            setSmallIcon(R.mipmap.ic_launcher)
        }.build()

        startForeground(
            NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    override fun onDestroy() {
        logger.info { "on destroy service" }
        super.onDestroy()
        //stopRecording()

        resultCode = 0
        resultData = null
    }

    override fun onBind(intent: Intent): IBinder {
        logger.info { "on bind service" }
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        logger.info { "on unbind service" }
        stopService()
        return super.onUnbind(intent)
    }

    private fun createImageReaderVirtualDisplay() {
        if (mMediaProjection != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val windowMetrics = windowManager.currentWindowMetrics
            val defaultDisplay =
                DisplayManagerCompat.getInstance(this).getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext = createDisplayContext(defaultDisplay!!)

            //val widthPixels = displayContext.resources.displayMetrics.widthPixels
            //获取的数据与实际不同，2400（物理高度）实际获取到是 2262（App高度）
            //val heightPixels = displayContext.resources.displayMetrics.heightPixels

            val widthPixels = windowMetrics.bounds.width()
            val heightPixels = windowMetrics.bounds.height()
            val densityDpi = displayContext.resources.displayMetrics.densityDpi

            mImageReader =
                ImageReader.newInstance(widthPixels, heightPixels, PixelFormat.RGBA_8888, 1)
            mImageReader!!.setOnImageAvailableListener({
                mImageAvailable = true
                logger.info { "mImageReader image available: $mImageAvailable" }
            }, null)
            mMediaProjection!!.registerCallback(MEDIA_PROJECTION_CALLBACK, null)
            mVirtualDisplayImageReader = mMediaProjection!!.createVirtualDisplay(
                "ImageReader", widthPixels, heightPixels, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader!!.surface, null, null
            )
        }
    }

    fun screenshot(): Bitmap? {
        check(mImageAvailable) { "screenshot: mImageAvailable is false" }
        check(mImageReader != null) { "screenshot: mImageReader is null" }

        return runCatching {
            val image: Image? = mImageReader!!.acquireLatestImage()
            image ?: return lastBitmap ?: throw Exception("Can't get image")
            val bitmap = image.toBitmap()
            image.close()
            logger.info { "screenshot success" }
            lastBitmap = bitmap
            runBlocking { saveBitmap(bitmap) }

            bitmap
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    suspend fun saveBitmap(bitmap: Bitmap) {
        val fileName = "screenshot_${System.currentTimeMillis()}.jpg"
        val file = File(App.context.externalCacheDir, fileName)
        withContext(Dispatchers.IO) {
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
            }
        }
        logger.info { "Screenshot saved to ${file.absolutePath}" }
    }

    fun stopRecording() {
        logger.info { "stopRecording" }
        mImageReader?.close()
        mImageReader = null
        mImageAvailable = false
        mMediaProjection?.unregisterCallback(MEDIA_PROJECTION_CALLBACK)
        mMediaProjection?.stop()
        mMediaProjection = null
        running = false
    }

    fun stopService() {
        logger.info { "stopService" }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopRecording()
        stopSelf()
    }

    inner class MediaProjectionServiceBinder : Binder() {
        fun getService(): MediaProjectionService = this@MediaProjectionService

        // 提供停止服务的接口
        fun stopService() {
            this@MediaProjectionService.stopService()
        }
    }
}
