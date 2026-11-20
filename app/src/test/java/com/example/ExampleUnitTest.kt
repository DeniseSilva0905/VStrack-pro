package com.example

import com.example.audio.ChordTransposer
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTransposeKey() {
        assertEquals("C#", ChordTransposer.transposeKey("C", 1))
        assertEquals("D", ChordTransposer.transposeKey("C", 2))
        assertEquals("B", ChordTransposer.transposeKey("C", -1))
        assertEquals("G", ChordTransposer.transposeKey("F#", 1))
    }

    @Test
    fun testTransposeChordsText() {
        val original = "| C | G/B | Am7 | F |"
        val transposed = ChordTransposer.transposeText(original, 2)
        assertEquals("| D | A/C# | Bm7 | G |", transposed)
    }

    @Test
    fun testTransposeWithTomPrefix() {
        val original = "Tom: C\n| C | G |"
        val transposed = ChordTransposer.transposeText(original, 1)
        assertTrue(transposed.contains("Tom: C#"))
        assertTrue(transposed.contains("| C# | G# |"))
    }
}
