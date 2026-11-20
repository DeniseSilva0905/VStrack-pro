package com.example.audio

object ChordTransposer {
    private val NOTES_SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val NOTES_FLAT = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_MAP = mapOf(
        "C" to 0, "B#" to 0,
        "C#" to 1, "Db" to 1,
        "D" to 2,
        "D#" to 3, "Eb" to 3,
        "E" to 4, "Fb" to 4,
        "F" to 5, "E#" to 5,
        "F#" to 6, "Gb" to 6,
        "G" to 7,
        "G#" to 8, "Ab" to 8,
        "A" to 9,
        "A#" to 10, "Bb" to 10,
        "B" to 11, "Cb" to 11
    )

    fun transposeKey(originalKey: String, semitones: Int): String {
        val cleanKey = originalKey.trim()
        val index = NOTE_MAP[cleanKey] ?: return originalKey
        val newIndex = (index + semitones).mod(12)
        val preferFlat = cleanKey.contains("b")
        return if (preferFlat) NOTES_FLAT[newIndex] else NOTES_SHARP[newIndex]
    }

    /**
     * Transposes all chords in a text sheet.
     * Detects chord tokens like [Intro], | C | G/B | Am7 | F | or standard chord lines.
     */
    fun transposeText(content: String, semitones: Int): String {
        if (semitones == 0 || content.isBlank()) return content

        val chordRegex = Regex("""\b([A-G][b#]?)((?:m|maj|min|dim|aug|sus|add|[0-9]|b|#|\+)*)(?:/([A-G][b#]?))?\b""")

        return content.lines().joinToString("\n") { line ->
            var processedLine = line
            if (line.contains("Tom:", ignoreCase = true)) {
                val keyRegex = Regex("""Tom:\s*([A-G][b#]?)""", RegexOption.IGNORE_CASE)
                processedLine = keyRegex.replace(processedLine) { match ->
                    val key = match.groupValues[1]
                    "Tom: ${transposeKey(key, semitones)}"
                }
            }

            val isChordHeavy = line.contains("|") || line.trim().startsWith("[") || looksLikeChordLine(line)

            if (isChordHeavy) {
                chordRegex.replace(processedLine) { match ->
                    val root = match.groupValues[1]
                    val modifier = match.groupValues[2]
                    val bass = match.groupValues[3]

                    val newRoot = transposeKey(root, semitones)
                    val newBass = if (bass.isNotEmpty()) {
                        "/" + transposeKey(bass, semitones)
                    } else ""

                    "$newRoot$modifier$newBass"
                }
            } else {
                processedLine
            }
        }
    }

    private fun looksLikeChordLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val words = trimmed.split(Regex("""\s+"""))
        if (words.isEmpty()) return false

        var chordCount = 0
        for (w in words) {
            val clean = w.trim('[', ']', '(', ')', '|', ',')
            if (clean.matches(Regex("""^[A-G][b#]?(m|maj|min|dim|aug|sus|add|[0-9]|b|#|\+)*(?:/[A-G][b#]?)?$"""))) {
                chordCount++
            }
        }
        return chordCount.toFloat() / words.size >= 0.5f
    }
}
