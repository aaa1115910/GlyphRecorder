package dev.aaa1115910.glyphrecorder.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.services.MediaProjectionService
import dev.aaa1115910.glyphrecorder.util.BitmapUtil
import dev.aaa1115910.glyphrecorder.util.GlyphData
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.ShizukuConfig
import dev.aaa1115910.glyphrecorder.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat


data class FloatingToolboxState(
    val context: Context,
    val scope: CoroutineScope,
    val capturedGlyphs: SnapshotStateList<CapturedGlyph>,
    val matchedGlyphs: SnapshotStateList<String>,
    var mediaProjectionService: MediaProjectionService?,
    val onStopAutoCapture: () -> Unit,
    val onHapticFeedback: (HapticFeedbackType) -> Unit
) {
    companion object {
        val logger = KotlinLogging.logger { }
    }

    var targetGlyphSize = 0

    // 现在接收 name + index 两个值
    val onAddGlyph: (String, Int) -> Unit = { glyph, glyphIndex ->
        logger.info { "Adding glyph: $glyph index=$glyphIndex" }
        if (glyph.isNotEmpty()) {
            capturedGlyphs.add(CapturedGlyph(glyph, glyphIndex))
            logger.info { "Glyph added: $glyph index=$glyphIndex" }
        } else {
            logger.warn { "Attempted to add an empty glyph" }
        }

        // check matched glyphs
        if (targetGlyphSize > 0 && capturedGlyphs.size != targetGlyphSize) {
            val matched = if (targetGlyphSize >= 3) {
                GlyphData.matchSequenceWithIndex(targetGlyphSize, capturedGlyphs.toList())
            } else {
                // 当顶部六边形数量小于 3 时，忽略 index，只按 name 匹配
                GlyphData.matchSequence(targetGlyphSize, capturedGlyphs.map { it.name })
            }
            if (matched.size == 1) {
                matchedGlyphs.clear()
                matchedGlyphs.addAll(matched[0])
                logger.info { "Find matched glyphs: ${matched[0]}" }
                stopAutoCapture()
                onStopAutoCapture()
            }
        } else {
            //matchedGlyphs.clear()
        }
    }

    val onClearGlyphs: () -> Unit = {
        logger.info { "Clearing capturedGlyphs" }
        capturedGlyphs.clear()
        matchedGlyphs.clear()
        targetGlyphSize = 0
    }

    val onToast: (String) -> Unit = {
        scope.launch {
            withContext(Dispatchers.Main) {
                it.toast(context)
            }
        }
    }

    val onLongtimeNoGlyph: () -> Unit = {
        logger.info { "No glyph detected for a long time, stopping auto capture" }
        onHapticFeedback(HapticFeedbackType.Reject)
        onStopAutoCapture()
        onToast(context.getString(R.string.floating_window_long_time_no_glyph))
    }

    suspend fun takeScreenshotAsync(): ScreenshotResult? {
        val time = System.currentTimeMillis()
        val bitmap = when (Prefs.workingMode) {
            WorkingMode.MediaProjection -> {
                if (mediaProjectionService == null) {
                    logger.info { "MediaProjectionService is not bound, cannot take screenshot" }
                    return null
                }
                logger.info { "taking screenshot with MediaProjection" }
                mediaProjectionService!!.screenshot() ?: return null
            }

            WorkingMode.Shizuku -> {
                if (!ShizukuConfig.isShizukuConnected || ShizukuConfig.screenCaptureService == null) {
                    logger.info { "Shizuku is not connected or ScreenCaptureService is not available" }
                    return null
                }
                logger.info { "taking screenshot with Shizuku" }
                val result =
                    ShizukuConfig.screenCaptureService!!.takeScreenshotBitmapArray() ?: return null
                BitmapFactory.decodeStream(ByteArrayInputStream(result))
            }
        }
        logger.info { "screenshot took ${System.currentTimeMillis() - time} ms [${Prefs.workingMode}]" }

        val (hexagon, glyph) = withContext(Dispatchers.IO) {
            val hexagon = async { BitmapUtil.parseHexagon(bitmap) }
            val glyph = async { BitmapUtil.parseGlyph(bitmap) }
            hexagon.await() to glyph.await()
        }
        logger.info { "Screenshot taken: Hexagon=${hexagon}, Glyph=$glyph" }
        val (hexagonCount, glyphIndex) = hexagon
        return ScreenshotResult(
            hexagonCount = hexagonCount,
            glyph = glyph,
            glyphIndex = glyphIndex
        )
    }

    suspend fun takeScreenshot() {
        val (hexagon, glyph, glyphIndex) = takeScreenshotAsync() ?: return
        logger.info { "hexagon: $hexagon" }
        logger.info { "glyph: $glyph" }
        logger.info { "glyph index: $glyphIndex" }
        if (glyph != null) {
            onAddGlyph(glyph, glyphIndex)
            logger.info { "Glyph added: $glyph" }
        } else {
            logger.info { "Failed to parse glyph" }
        }
    }

    private var autoCaptureJob: Job? = null
    var interval = 1000L
    var autoRunning = false

    enum class GlyphCaptureState {
        Idle, Capturing, Stopped
    }

    var glyphCaptureState = GlyphCaptureState.Idle
    var idleCounter = 0
    var captureCounter = 0
    var lastTimestamp = 0

    suspend fun jobContent() {
        val timestamp = System.currentTimeMillis()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(java.util.Date())
        val (hexagon, glyph, glyphIndex) = takeScreenshotAsync() ?: return
        if (hexagon != 0 && glyph == null && hexagon > capturedGlyphs.size) return

        when (glyphCaptureState) {
            GlyphCaptureState.Idle -> {
                // 开始接收符号序列
                if (hexagon != 0) {
                    glyphCaptureState = GlyphCaptureState.Capturing
                    targetGlyphSize = hexagon
                }
                if (hexagon == 0) {
                    idleCounter++
                    // 如果连续多次没有符号，进入停止状态，截图间隔 200ms 一次，此处为 10 秒
                    if (idleCounter > 5 * 10) {
                        glyphCaptureState = GlyphCaptureState.Stopped
                        onLongtimeNoGlyph()
                    }
                }
            }

            GlyphCaptureState.Capturing -> {
                // 画符结束
                if (hexagon == 0) glyphCaptureState = GlyphCaptureState.Stopped
                // 画符未结束但数量已满
                if (hexagon != 0 && capturedGlyphs.size == hexagon) glyphCaptureState =
                    GlyphCaptureState.Stopped
                // 画符未结束且数量未满
                if (hexagon != 0 && glyph == null) {
                    captureCounter++
                    targetGlyphSize = hexagon
                    // 如果连续多次没有符号，进入停止状态，正常情况下只会有 3s 时间 glyph==null，截图间隔 200ms 一次，此处为 3+10 秒
                    if (captureCounter > 5 * 13) {
                        glyphCaptureState = GlyphCaptureState.Stopped
                        onLongtimeNoGlyph()
                    }
                }
            }

            GlyphCaptureState.Stopped -> {
                stopAutoCapture()
            }
        }

        logger.info { "[$time] AutoCapture job content: glyphCaptureState=$glyphCaptureState, glyph=$glyph, glyphIndex=$glyphIndex" }
        if (autoRunning && glyphCaptureState == GlyphCaptureState.Capturing && glyph != null) {
            // 当 hexagon 数量大于等于 3 时才加入 glyph index 判断
            if (hexagon >= 3) {
                if (capturedGlyphs.lastOrNull()?.name != glyph || capturedGlyphs.lastOrNull()?.index != glyphIndex) {
                    if (timestamp > lastTimestamp) {
                        onAddGlyph(glyph, glyphIndex)
                    } else {
                        logger.info { "Skip adding glyph due to expired glyph: $glyph" }
                    }
                }
            } else {
                if (capturedGlyphs.lastOrNull()?.name != glyph) {
                    if (timestamp > lastTimestamp) {
                        onAddGlyph(glyph, glyphIndex)
                    } else {
                        logger.info { "Skip adding glyph due to expired glyph: $glyph" }
                    }
                }
            }
        }
    }

    fun startAutoCapture() {
        if (autoCaptureJob?.isActive == true) return // 已在自动捕捉

        onClearGlyphs()
        idleCounter = 0
        interval = 200L
        glyphCaptureState = GlyphCaptureState.Idle
        autoRunning = true
        logger.info { "Starting auto capture with interval: ${interval}ms" }

        autoCaptureJob = scope.launch {
            while (autoRunning) {
                scope.launch(Dispatchers.IO) {
                    jobContent()
                }
                delay(interval)
            }
        }
    }

    fun stopAutoCapture() {
        logger.info { "Stopping auto capture" }
        onStopAutoCapture()
        autoCaptureJob?.cancel()
        autoRunning = false
        autoCaptureJob = null
    }
}

@Composable
fun rememberFloatingToolboxState(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    onStopAutoCapture: () -> Unit
): FloatingToolboxState {
    val haptic = LocalHapticFeedback.current
    val logger = KotlinLogging.logger("rememberFloatingToolboxState")

    var mediaProjectionService by remember { mutableStateOf<MediaProjectionService?>(null) }
    val capturedGlyphs = remember { mutableStateListOf<CapturedGlyph>() }
    val matchedGlyphs = remember { mutableStateListOf<String>() }

    val onHapticFeedback: (HapticFeedbackType) -> Unit = { type ->
        haptic.performHapticFeedback(type)
    }

    DisposableEffect(Unit) {
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mediaProjectionService =
                    (service as MediaProjectionService.MediaProjectionServiceBinder).getService()
                logger.info { "MediaProjectionService connected in FloatingToolboxState" }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                logger.info { "MediaProjectionService disconnected in FloatingToolboxState" }
            }
        }

        if (Prefs.workingMode == WorkingMode.MediaProjection && mediaProjectionService == null) {
            context.bindService(
                Intent(context, MediaProjectionService::class.java),
                conn,
                Context.BIND_AUTO_CREATE
            )
            logger.info { "MediaProjectionService bound in FloatingToolboxState" }
        }

        onDispose {
            logger.info { "FloatingToolboxState onDispose" }
            if (Prefs.workingMode == WorkingMode.MediaProjection && mediaProjectionService != null) {
                context.unbindService(conn)
            }
        }
    }

    return remember(
        context,
        scope,
        mediaProjectionService
    ) {
        FloatingToolboxState(
            context,
            scope,
            capturedGlyphs,
            matchedGlyphs,
            mediaProjectionService,
            onStopAutoCapture,
            onHapticFeedback
        )
    }
}

data class ScreenshotResult(
    val hexagonCount: Int,
    val glyph: String?,
    val glyphIndex: Int
)

data class CapturedGlyph(
    val name: String,
    val index: Int
)

data class CapturedGlyphSequence(
    val glyphs: List<CapturedGlyph>
)