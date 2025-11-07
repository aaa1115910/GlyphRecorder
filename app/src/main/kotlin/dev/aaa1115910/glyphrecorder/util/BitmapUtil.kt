package dev.aaa1115910.glyphrecorder.util

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import androidx.compose.ui.graphics.Color
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opencv.core.Point
import java.io.ByteArrayOutputStream
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

    /**
     * 识别 [bitmap] 中的正六边形
     *
     * @return 返回正六边形的数量和黄色的位置（glyph位置指示）,如果没有黄色则返回 -1
     */
    suspend fun parseHexagon(bitmap: Bitmap): Pair<Int, Int> {
        val hexagons = mutableListOf<Pair<Point, Color>>()
        val spentTime = measureTimeMillis {
            val topQuarter = createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height / 5)
            hexagons.addAll(OpenCvUtil.detectHexagonsWithColor(topQuarter))
            topQuarter.recycle()
            logger.info { "Detected hexagons: $hexagons" }
        }
        val hexagonCount = hexagons.size
        val sortedHexagons = hexagons.sortedBy { it.first.x }

        // 计算离群颜色索引（基于 RGB 空间欧氏距离）
        val glyphHexagonIndex =
            ColorUtil.findMostDifferentColorIndex(sortedHexagons.map { it.second })

        logger.info { "Hexagon count recognition took $spentTime ms" }
        return hexagonCount to glyphHexagonIndex
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
