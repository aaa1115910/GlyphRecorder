package dev.aaa1115910.glyphrecorder

import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import dev.aaa1115910.glyphrecorder.util.Circle
import dev.aaa1115910.glyphrecorder.util.OpenCvUtil
import dev.aaa1115910.glyphrecorder.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import kotlin.system.measureTimeMillis


class GlyphDetect {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    @Before
    @Test
    fun `detect circles`() {
        val image = "glyph_hack_0.png"
        val bitmap = BitmapFactory.decodeStream(openInputStream(image))
        val circles = OpenCvUtil.detectCircles(bitmap)
        logger.info { "Circles: $circles" }
        for ((index, pair) in circles.withIndex()) {
            logger.info { "Circle $index: x=${pair.first}, y=${pair.second}" }
        }
        assertEquals("Circle count", 11, circles.size)
        Prefs.circles = circles
        OpenCvUtil.updatePrefCircles(circles)
    }

    @Test
    fun `detect activated lines`() {
        val image = "glyph_hack_1.png"
        val bitmap = BitmapFactory.decodeStream(openInputStream(image))
        val lines = OpenCvUtil.detectLines(bitmap)
        logger.info { "Activated Lines: $lines" }
    }

    @Test
    fun `detect filtered lines`() {
        val image = "glyph_hack_1.png"
        val bitmap = BitmapFactory.decodeStream(openInputStream(image))
        val lines = OpenCvUtil.detectFilteredLines(bitmap)
        logger.info { "Filtered Lines: $lines" }
    }

    @Test
    fun `detect glyphs`() {
        val result = listOf(null, "knowledge", "help", "gain", "victory", null, null)
        for (i in 0..6) {
            val image = "glyph_hack_$i.png"
            val bitmap = BitmapFactory.decodeStream(openInputStream(image))
            var glyphs: List<String>
            val time = measureTimeMillis {
                glyphs = OpenCvUtil.detectGlyphs(bitmap)
            }
            logger.info { "Glyphs: $glyphs" }
            logger.info { "Detection took $time ms" }
            assertEquals("Glyph $i", result[i], glyphs.firstOrNull())
        }
    }

    @Test
    fun `detect hexagons`() {
        val result = listOf(0, 4, 4, 4, 4, 4, 4)
        for (i in 0..6) {
            val image = "glyph_hack_$i.png"
            logger.info { "image: $image" }
            val bitmap = BitmapFactory.decodeStream(openInputStream(image))
            val topQuarter = createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height / 5)
            var hexagonCount = 0
            val time = measureTimeMillis {
                val hexagons = OpenCvUtil.detectHexagonsWithColor(topQuarter)
                hexagonCount = hexagons.size
                hexagons.forEach { (point, color) ->
                    val colorHex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                    logger.info { "Hexagon at ${point.x.toInt()} with color $colorHex" }
                }
            }
            logger.info { "Hexagon count: $hexagonCount" }
            logger.info { "Detection took $time ms" }
            assertEquals("Hexagon $i", result[i], hexagonCount)
        }
    }

    private fun openInputStream(filename: String): InputStream {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(filename)
    }
}
