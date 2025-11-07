package dev.aaa1115910.glyphrecorder

import androidx.compose.ui.graphics.Color
import dev.aaa1115910.glyphrecorder.util.ColorUtil
import kotlin.test.Test

class ColorTest {
    @Test
    fun `t1`() {
        val colors =
            listOf(Color(0xFF272726), Color(0xFF272726), Color(0xFF214455), Color(0xFF272726))
        val index = ColorUtil.findMostDifferentColorIndex(colors)
        println(colors)
        println(index)
    }

    @Test
    fun `t2`() {
        val colors =
            listOf(Color(0xFF272726), Color(0xFF272726), Color(0xFF272726), Color(0xFF272727))
        val index = ColorUtil.findMostDifferentColorIndex(colors)
        println(colors)
        println(index)
    }

    @Test
    fun `t3`() {
        val colors =
            listOf(Color(0xFF262624), Color(0xFF262624), Color(0xFF262624), Color(0xFF1b3c4c))
        val index = ColorUtil.findMostDifferentColorIndex(colors)
        println(colors)
        println(index)
    }
}