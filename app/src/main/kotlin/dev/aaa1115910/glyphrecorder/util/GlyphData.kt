package dev.aaa1115910.glyphrecorder.util

import android.content.Context


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

    fun matchSequence(size: Int, glyphs: List<String>): List<List<String>> {
        return glyphSequenceMap[size]?.filter { seq ->
            containsOrdered(seq, glyphs)
        } ?: emptyList()
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
