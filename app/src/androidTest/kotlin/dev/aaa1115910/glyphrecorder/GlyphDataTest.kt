package dev.aaa1115910.glyphrecorder

import dev.aaa1115910.glyphrecorder.util.GlyphData
import org.junit.Test

class GlyphDataTest {
    @Test
    fun `match glyph sequence`(){
        val size=5
        val sequence=listOf("defend","shapers")
        val matchedSequences = GlyphData.matchSequence(size, sequence)
        println("Matched sequences of size $size for $sequence: $matchedSequences")
    }
    @Test
    fun `match glyph sequence2`(){
        val size=4
        val sequence=listOf("interrupt","message","gain")
        val matchedSequences = GlyphData.matchSequence(size, sequence)
        println("Matched sequences of size $size for $sequence: $matchedSequences")
    }
}