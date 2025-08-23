package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import dev.aaa1115910.glyphrecorder.App
import kotlin.collections.forEach


object GlyphData {
    val glyphLines = mutableMapOf<String, String>()
    val glyphSequence = mutableListOf<List<String>>()
    val glyphSequenceMap = mutableMapOf<Int, MutableList<List<String>>>()

    init {
        initGlyphData()
    }

    private fun initGlyphData() {
        readGlyphCsv(App.Companion.context)
        readGlyphLineCsv(App.Companion.context)
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
}