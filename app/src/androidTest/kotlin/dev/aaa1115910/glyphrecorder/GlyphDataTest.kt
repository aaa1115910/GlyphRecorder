package dev.aaa1115910.glyphrecorder

import dev.aaa1115910.glyphrecorder.ui.CapturedGlyph
import dev.aaa1115910.glyphrecorder.util.GlyphData
import org.junit.Test

class GlyphDataTest {
    @Test
    fun `match glyph sequence`() {
        val size = 5
        val sequence = listOf("defend", "shapers")
        val matchedSequences = GlyphData.matchSequence(size, sequence)
        println("Matched sequences of size $size for $sequence: $matchedSequences")
    }

    @Test
    fun `match glyph sequence2`() {
        val size = 4
        val sequence = listOf("interrupt", "message", "gain")
        val matchedSequences = GlyphData.matchSequence(size, sequence)
        println("Matched sequences of size $size for $sequence: $matchedSequences")
    }

    @Test
    fun `match glyph sequence3`() {
        val size = 4
        val sequence = listOf("complex", "shapers", "civilization")
        val matchedSequences = GlyphData.matchSequence(size, sequence)
        println("Matched sequences of size $size for $sequence: $matchedSequences")
        val sequenceWithIndex = listOf(
            CapturedGlyph("complex", 0),
            CapturedGlyph("shapers", 1),
            CapturedGlyph("civilization", 2)
        )
        val matchedSequencesWithIndex = GlyphData.matchSequenceWithIndex(size, sequenceWithIndex)
        println("Matched sequences with index of size $size for $sequenceWithIndex: $matchedSequencesWithIndex")
    }
}