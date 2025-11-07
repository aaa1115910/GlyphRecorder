package dev.aaa1115910.glyphrecorder.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toColorLong
import okhttp3.internal.toHexString
import kotlin.math.sqrt

object ColorUtil {
    //找出color[]中差异最大的一个颜色的下标
    fun findMostDifferentColorIndex(colors: List<Color>): Int {
        // 计算离群颜色索引（基于 RGB 空间欧氏距离）
        val n = colors.size
        if (n < 3) return -1

        fun dist(a: Color, b: Color): Float {
            val dr = a.red - b.red
            val dg = a.green - b.green
            val db = a.blue - b.blue
            return sqrt(dr * dr + dg * dg + db * db)
        }

        val means = DoubleArray(n) { i ->
            var sum = 0.0
            for (j in 0 until n) if (i != j) sum += dist(colors[i], colors[j])
            sum / (n - 1)
        }

        val maxIndex = means.indices.maxByOrNull { means[it] } ?: -1
        if (maxIndex == -1) return -1
        val maxMean = means[maxIndex]
        val secondMax = means.indices.filter { it != maxIndex }.maxOf { means[it] }

        // 判定阈值：要么绝对差异足够大（>0.1），要么比第二大均值大得多（>1.5倍）
        val result = if (maxMean > kotlin.math.max(0.1, secondMax * 1.5)) maxIndex else -1
        println("findMostDifferentColorIndex ${colors.map { it.toColorLong().toHexString() }} -> $result")
        return result
    }
}