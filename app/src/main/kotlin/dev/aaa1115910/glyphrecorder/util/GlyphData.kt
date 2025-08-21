package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import dev.aaa1115910.glyphrecorder.App


object GlyphData{
    val glyphLines = mutableMapOf<String, String>()

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

    }

    private fun readGlyphLineCsv(context: Context) {
        val filename = "glyph_line.csv"
        val text = context.assets.open(filename).bufferedReader().use { it.readText() }
        text.lines().forEach { line ->
            if (line.isEmpty()) return@forEach
            val (glyphName, glyphPath) = line.split(",")
            glyphLines[glyphName] = glyphPath
        }
    }
}