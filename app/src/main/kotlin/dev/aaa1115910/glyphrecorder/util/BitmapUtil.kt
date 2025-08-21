package dev.aaa1115910.glyphrecorder.util

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.system.measureTimeMillis


object BitmapUtil {
    private val logger = KotlinLogging.logger { }

    /**
     * 识别 [bitmap] 中的 Glyph 名称
     */
    suspend fun parseGlyph(bitmap: Bitmap): String? {
        var glyph: String? = null
        val spentTime = measureTimeMillis {
            val glyphs = OpenCvUtil.detectGlyphs(bitmap)
            logger.info { "Detected glyphs: $glyphs" }
            glyph = glyphs.firstOrNull()
        }
        logger.info { "Glyph recognition took $spentTime ms" }
        return glyph
    }

    /**
     * 识别 [bitmap] 中的正六边形数量
     */
    suspend fun parseHexagonCount(bitmap: Bitmap): Int {
        var hexagonCount = 0
        val spentTime = measureTimeMillis {
            val topQuarter = createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height / 5)
            hexagonCount = OpenCvUtil.detectHexagons(topQuarter)
            topQuarter.recycle()
            logger.info { "Detected hexagon counts: $hexagonCount" }
        }
        logger.info { "Hexagon count recognition took $spentTime ms" }
        return hexagonCount
    }
}

/**
 * 将 [Bitmap] 压缩为字节数组 [ByteArray]
 *
 * @param format 压缩格式
 * @param quality 压缩质量，范围 0-100
 * @return 压缩后的字节数组
 */
fun Bitmap.compressToByteArray(format: Bitmap.CompressFormat, quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(format, quality, stream)
    return stream.toByteArray()
}
