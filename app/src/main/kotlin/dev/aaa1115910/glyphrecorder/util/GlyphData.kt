package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import dev.aaa1115910.glyphrecorder.ui.CapturedGlyph


object GlyphData {
    val glyphLines = mutableMapOf<String, String>()
    val glyphSequence = mutableListOf<List<String>>()
    val glyphSequenceMap = mutableMapOf<Int, MutableList<List<String>>>()

    fun initGlyphData(context: Context) {
        readGlyphCsv(context)
        readGlyphLineCsv(context)
    }

    private fun readGlyphCsv(context: Context) {
        val filename = "glyph.csv"
        val text = context.assets.open(filename).bufferedReader().use { it.readText() }
        text.lines().forEach { line ->
            if (line.isEmpty()) return@forEach
            val glyphs = line.split(",").filter { it.isNotEmpty() }
            glyphSequence.add(glyphs)
        }
    }

    private fun readGlyphLineCsv(context: Context) {
        val filename = "glyph_line.csv"
        val text = context.assets.open(filename).bufferedReader().use { it.readText() }
        text.lines().forEach { line ->
            if (line.isEmpty()) return@forEach
            val (glyphName, glyphPath) = line.split(",")
            glyphLines[glyphName] = glyphPath
        }
        glyphSequenceMap.clear()
        glyphSequence.forEach { seq ->
            val size = seq.size
            if (glyphSequenceMap.containsKey(size)) {
                glyphSequenceMap[size]!!.add(seq)
            } else {
                glyphSequenceMap[size] = mutableListOf(seq)
            }
        }
    }

    /**
     * 按名称列表匹配
     * @param size 序列长度
     * @param glyphs 待匹配的名称列表
     * @return 返回所有匹配的名称序列列表
     */
    fun matchSequence(size: Int, glyphs: List<String>): List<List<String>> {
        return glyphSequenceMap[size]?.filter { seq ->
            containsOrdered(seq, glyphs)
        } ?: emptyList()
    }

    /**
     * 按名称和索引匹配
     * @param size 序列长度
     * @param glyphSequence 待匹配的名称和索引列表
     * @return 返回所有匹配的名称序列列表
     */
    fun matchSequenceWithIndex(size: Int, glyphSequence: List<CapturedGlyph>): List<List<String>> {
        return (glyphSequenceMap[size]?.filter { seq ->
            containsOrderedWithIndex(seq, glyphSequence)
        } ?: emptyList())
    }

    private fun containsOrdered(seq: List<String>, glyphs: List<String>): Boolean {
        if (glyphs.isEmpty()) return true
        var idx = 0
        for (item in seq) {
            if (item == glyphs[idx]) {
                idx++
                if (idx == glyphs.size) return true
            }
        }
        return false
    }

    /**
     * 按名称和索引匹配序列
     * @param seq 预设的glyph序列
     * @param glyphSequence 待匹配的名称和索引列表
     * @return 如果匹配则返回 true，否则返回 false
     */
    private fun containsOrderedWithIndex(
        seq: List<String>,
        glyphSequence: List<CapturedGlyph>
    ): Boolean {
        if (glyphSequence.isEmpty()) return true
        var matchedCount = 0
        for (i in seq.indices) {
            val seqItem = seq[i]
            val (name, requiredIndex) = glyphSequence[matchedCount]
            if (seqItem == name) {
                // 如果提供了特定索引且不匹配则跳过该匹配
                if (requiredIndex >= 0 && i != requiredIndex) {
                    // continue searching same glyphPairs[matchedCount] 在 seq 的后续位置
                    continue
                }
                matchedCount++
                if (matchedCount == glyphSequence.size) return true
            }
        }
        return false
    }

    fun nameToPaths(name: String): List<Pair<Circle, Circle>> {
        val glyphPath = glyphLines[name] ?: return emptyList()
        val glyphPathTuples = glyphPath.windowed(2)
        return glyphPathTuples.map {
            var start = it[0]
            var end = it[1]
            if (start > end) {
                start = end.also { end = start }
            }
            Pair(Circle.fromId(start.toString()), Circle.fromId(end.toString()))
        }
    }
}
