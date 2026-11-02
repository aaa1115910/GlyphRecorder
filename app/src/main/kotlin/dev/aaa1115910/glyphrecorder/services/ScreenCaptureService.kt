package dev.aaa1115910.glyphrecorder.services

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.util.Log
import com.genymobile.scrcpy.Options
import com.genymobile.scrcpy.video.ScreenCapture
import com.genymobile.scrcpy.video.SurfaceCapture
import dev.aaa1115910.glyphrecorder.IScreenCaptureService
import dev.aaa1115910.glyphrecorder.util.OpenCvUtil
import dev.aaa1115910.glyphrecorder.util.compressToByteArray
import dev.aaa1115910.glyphrecorder.util.toBitmap
import java.io.File
import java.text.SimpleDateFormat
import kotlin.system.exitProcess

class ScreenCaptureService : IScreenCaptureService.Stub() {
    companion object {
        private const val LOG_FILENAME = "glyph_recorder.log"
    }

    private var surfaceCapture: SurfaceCapture? = null
    private var mImageReader: ImageReader? = null
    @Volatile private var mImageAvailable: Boolean = false
    private var lastBitmap: Bitmap? = null
    private var client: IBinder? = null
    private var destroyed = false
    private val clientDeath = IBinder.DeathRecipient { destroy() }

    @Synchronized
    override fun attachClient(client: IBinder) {
        check(!destroyed) { "Service has been destroyed" }
        check(this.client == null || this.client == client) { "Service already has an owner" }
        if (this.client == client) return
        // Link before starting capture: a dead client must never leave an unowned display.
        client.linkToDeath(clientDeath, 0)
        this.client = client
    }

    @Synchronized
    override fun startScreenCapture(): Boolean {
        check(!destroyed && client?.isBinderAlive == true) { "No live capture client" }
        closeResources()
        return try {
            log("Starting screen capture")
            val scrcpyServerParams = "3.3.1"
            val options = Options.parse(scrcpyServerParams)
            surfaceCapture = ScreenCapture(null, options)
            surfaceCapture?.prepare()
            val size = surfaceCapture!!.size
            mImageReader = ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 1)
            mImageReader?.setOnImageAvailableListener(
                { mImageAvailable = true },
                Handler(Looper.getMainLooper())
            )
            surfaceCapture?.start(mImageReader?.surface)
            surfaceCapture != null && mImageReader != null
        } catch (error: Throwable) {
            closeResources()
            Log.e("ScreenCaptureService", "Unable to start screen capture", error)
            false
        }
    }

    @Synchronized
    override fun stopScreenCapture() = closeResources()

    @Synchronized
    override fun destroy() {
        if (destroyed) return
        destroyed = true
        try {
            runCatching { client?.unlinkToDeath(clientDeath, 0) }
            client = null
            closeResources()
        } finally {
            // A Shizuku user service is a separate app_process, not an Android Service.
            exitProcess(0)
        }
    }

    private fun closeResources() {
        runCatching { log("Closing resources") }
        mImageAvailable = false
        val capture = surfaceCapture
        val reader = mImageReader
        surfaceCapture = null
        mImageReader = null
        // A failure in one release must not prevent releasing the other resources.
        runCatching { capture?.stop() }.onFailure { Log.w("ScreenCaptureService", "Capture stop failed", it) }
        runCatching { capture?.release() }.onFailure { Log.w("ScreenCaptureService", "Capture release failed", it) }
        runCatching { reader?.close() }.onFailure { Log.w("ScreenCaptureService", "ImageReader close failed", it) }
        lastBitmap?.recycle()
        lastBitmap = null
    }

    @Synchronized
    override fun updateCircles(circlesRawString: String) {
        val circles = circlesRawString.split(",")
            .filter { it.isNotEmpty() }
            .map { it.split(":").let { pair -> pair[0].toInt() to pair[1].toInt() } }
        OpenCvUtil.updatePrefCircles(circles)
        log("Updated circles: $circles")
    }

    @Synchronized
    override fun takeScreenshotBitmapArray(): ByteArray? {
        log("Taking screenshot as Bitmap array")
        val bitmap = takeScreenshotBitmap() ?: return null
        val bytes = bitmap.compressToByteArray(Bitmap.CompressFormat.JPEG, 10)
        return bytes
    }

    private fun takeScreenshotBitmap(): Bitmap? {
        if (surfaceCapture == null) return null
        if (!mImageAvailable) return null

        return runCatching {
            val image = mImageReader?.acquireLatestImage()
            image ?: return lastBitmap ?: throw Exception("Can't get image")
            val bitmap = image.toBitmap()
            image.close()
            log("screenshot success")
            lastBitmap = bitmap
            bitmap
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    private fun log(message: String) {
        val logFile = File("/data/local/tmp/$LOG_FILENAME")
        val maxSize = 5 * 1024 * 1024 // 5MB

        if (logFile.exists() && logFile.length() > maxSize) {
            val content = logFile.readText()
            val halfIndex = content.length / 2
            val newContent = content.substring(halfIndex)
            logFile.writeText(newContent)
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(java.util.Date())
        logFile.appendText("[$timestamp] $message\n")
        //println(message)
    }
}