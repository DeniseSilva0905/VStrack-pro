package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipInputStream

data class RichRun(
    val text: String,
    val fontSizeSp: Float = 24f,
    val colorHex: String = "#FFFFFF",
    val fontFamily: String = "",
    val isBold: Boolean = false,
    val isItalic: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("text", text)
        obj.put("fontSizeSp", fontSizeSp.toDouble())
        obj.put("colorHex", colorHex)
        obj.put("fontFamily", fontFamily)
        obj.put("isBold", isBold)
        obj.put("isItalic", isItalic)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): RichRun {
            return RichRun(
                text = obj.optString("text", ""),
                fontSizeSp = obj.optDouble("fontSizeSp", 24.0).toFloat(),
                colorHex = obj.optString("colorHex", "#FFFFFF"),
                fontFamily = obj.optString("fontFamily", ""),
                isBold = obj.optBoolean("isBold", false),
                isItalic = obj.optBoolean("isItalic", false)
            )
        }
    }
}

data class RichParagraph(
    val alignment: String = "center", // "center", "left", "right"
    val runs: List<RichRun> = emptyList()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("alignment", alignment)
        val runsArray = JSONArray()
        runs.forEach { runsArray.put(it.toJson()) }
        obj.put("runs", runsArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): RichParagraph {
            val alignment = obj.optString("alignment", "center")
            val runsArray = obj.optJSONArray("runs") ?: JSONArray()
            val runs = mutableListOf<RichRun>()
            for (i in 0 until runsArray.length()) {
                val runObj = runsArray.optJSONObject(i)
                if (runObj != null) runs.add(RichRun.fromJson(runObj))
            }
            return RichParagraph(alignment, runs)
        }
    }
}

data class RichSlide(
    val slideNumber: Int,
    val title: String = "",
    val paragraphs: List<RichParagraph> = emptyList()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("slideNumber", slideNumber)
        obj.put("title", title)
        val paraArray = JSONArray()
        paragraphs.forEach { paraArray.put(it.toJson()) }
        obj.put("paragraphs", paraArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): RichSlide {
            val slideNumber = obj.optInt("slideNumber", 1)
            val title = obj.optString("title", "")
            val paraArray = obj.optJSONArray("paragraphs") ?: JSONArray()
            val paras = mutableListOf<RichParagraph>()
            for (i in 0 until paraArray.length()) {
                val pObj = paraArray.optJSONObject(i)
                if (pObj != null) paras.add(RichParagraph.fromJson(pObj))
            }
            return RichSlide(slideNumber, title, paras)
        }
    }
}

data class ParsedPowerPointResult(
    val title: String,
    val plainLyrics: String,
    val richJson: String,
    val slides: List<RichSlide>
)

object PowerPointLyricsParser {

    private const val TAG = "PPTLyricsParser"

    fun slidesToJson(slides: List<RichSlide>): String {
        val arr = JSONArray()
        slides.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    fun slidesFromJson(json: String): List<RichSlide> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<RichSlide>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) list.add(RichSlide.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing rich slides from JSON", e)
            emptyList()
        }
    }

    /**
     * Parses a PowerPoint (.pptx) file from a content Uri.
     * Extracts slides, font sizes, colors, font families, alignments, bold, italic.
     */
    fun parsePowerPoint(context: Context, uri: Uri, fallbackTitle: String = ""): ParsedPowerPointResult {
        val slideXmlMap = mutableMapOf<Int, String>()
        var extractedTitle = fallbackTitle

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // Check for slide XMLs: ppt/slides/slide1.xml, slide2.xml, etc.
                        val slideRegex = Regex("ppt/slides/slide([0-9]+)\\.xml", RegexOption.IGNORE_CASE)
                        val match = slideRegex.matchEntire(name)
                        if (match != null) {
                            val slideNum = match.groupValues[1].toIntOrNull() ?: 0
                            val reader = BufferedReader(InputStreamReader(zis))
                            val content = reader.readText()
                            slideXmlMap[slideNum] = content
                        } else if (name.equals("docProps/core.xml", ignoreCase = true)) {
                            // Extract title if available
                            try {
                                val reader = BufferedReader(InputStreamReader(zis))
                                val coreXml = reader.readText()
                                val titleMatch = Regex("<dc:title>([^<]+)</dc:title>").find(coreXml)
                                if (titleMatch != null && titleMatch.groupValues[1].isNotBlank()) {
                                    extractedTitle = titleMatch.groupValues[1].trim()
                                }
                            } catch (_: Exception) {}
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading pptx ZIP archive", e)
        }

        val sortedSlideIndices = slideXmlMap.keys.sorted()
        val slides = mutableListOf<RichSlide>()
        val plainLyricsBuilder = StringBuilder()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true

        for (slideNum in sortedSlideIndices) {
            val xml = slideXmlMap[slideNum] ?: continue
            val slideParagraphs = parseSlideXml(xml, factory)

            if (slideParagraphs.isNotEmpty()) {
                val slide = RichSlide(
                    slideNumber = slideNum,
                    title = "Slide $slideNum",
                    paragraphs = slideParagraphs
                )
                slides.add(slide)

                // Append to plain text lyrics
                if (plainLyricsBuilder.isNotEmpty()) {
                    plainLyricsBuilder.append("\n\n")
                }
                plainLyricsBuilder.append("--- Slide $slideNum ---\n")
                slideParagraphs.forEach { p ->
                    val line = p.runs.joinToString("") { it.text }.trim()
                    if (line.isNotBlank()) {
                        plainLyricsBuilder.append(line).append("\n")
                    }
                }
            }
        }

        // If no slides were parsed (e.g. non-pptx or empty), provide fallback
        if (slides.isEmpty()) {
            val fallbackRun = RichRun(
                text = "Letra importada de arquivo de apresentação.\nNenhum slide com texto formatado foi localizado.",
                fontSizeSp = 22f,
                colorHex = "#FFD700"
            )
            val fallbackSlide = RichSlide(
                slideNumber = 1,
                title = "Slide 1",
                paragraphs = listOf(RichParagraph("center", listOf(fallbackRun)))
            )
            slides.add(fallbackSlide)
            plainLyricsBuilder.append(fallbackRun.text)
        }

        val richJson = slidesToJson(slides)
        return ParsedPowerPointResult(
            title = extractedTitle.ifBlank { fallbackTitle },
            plainLyrics = plainLyricsBuilder.toString().trim(),
            richJson = richJson,
            slides = slides
        )
    }

    private fun parseSlideXml(xml: String, factory: XmlPullParserFactory): List<RichParagraph> {
        val paragraphs = mutableListOf<RichParagraph>()
        try {
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inParagraph = false
            var inRun = false
            var inRunProps = false
            var inSolidFill = false

            var currentAlignment = "center"
            var currentRuns = mutableListOf<RichRun>()

            // Run state
            var currentFontSizeSp = 24f
            var currentColorHex = "#FFFFFF"
            var currentFontFamily = ""
            var currentIsBold = false
            var currentIsItalic = false
            var runTextBuilder = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (name) {
                            "p" -> {
                                inParagraph = true
                                currentAlignment = "center"
                                currentRuns = mutableListOf()
                            }
                            "pPr" -> {
                                if (inParagraph) {
                                    val algn = parser.getAttributeValue(null, "algn")
                                    currentAlignment = when (algn) {
                                        "l" -> "left"
                                        "r" -> "right"
                                        "just" -> "justify"
                                        "ctr" -> "center"
                                        else -> "center"
                                    }
                                }
                            }
                            "r" -> {
                                if (inParagraph) {
                                    inRun = true
                                    currentFontSizeSp = 24f
                                    currentColorHex = "#FFFFFF"
                                    currentFontFamily = ""
                                    currentIsBold = false
                                    currentIsItalic = false
                                    runTextBuilder = StringBuilder()
                                }
                            }
                            "rPr" -> {
                                if (inRun) {
                                    inRunProps = true
                                    val szStr = parser.getAttributeValue(null, "sz")
                                    if (!szStr.isNullOrBlank()) {
                                        val szVal = szStr.toFloatOrNull() ?: 2400f
                                        // sz in OpenXML is in hundredths of a point (e.g. 3200 = 32pt)
                                        currentFontSizeSp = (szVal / 100f).coerceIn(14f, 48f)
                                    }
                                    val bStr = parser.getAttributeValue(null, "b")
                                    if (bStr == "1" || bStr.equals("true", ignoreCase = true)) {
                                        currentIsBold = true
                                    }
                                    val iStr = parser.getAttributeValue(null, "i")
                                    if (iStr == "1" || iStr.equals("true", ignoreCase = true)) {
                                        currentIsItalic = true
                                    }
                                }
                            }
                            "solidFill" -> {
                                if (inRunProps) {
                                    inSolidFill = true
                                }
                            }
                            "srgbClr" -> {
                                if (inSolidFill || inRunProps) {
                                    val valHex = parser.getAttributeValue(null, "val")
                                    if (!valHex.isNullOrBlank()) {
                                        currentColorHex = "#$valHex"
                                    }
                                }
                            }
                            "schemeClr" -> {
                                if (inSolidFill || inRunProps) {
                                    val scheme = parser.getAttributeValue(null, "val") ?: ""
                                    currentColorHex = mapSchemeColor(scheme)
                                }
                            }
                            "latin" -> {
                                if (inRunProps) {
                                    val tf = parser.getAttributeValue(null, "typeface")
                                    if (!tf.isNullOrBlank()) {
                                        currentFontFamily = tf
                                    }
                                }
                            }
                            "t" -> {
                                if (inRun) {
                                    val text = parser.nextText()
                                    runTextBuilder.append(text)
                                }
                            }
                            "br" -> {
                                if (inParagraph) {
                                    if (inRun && runTextBuilder.isNotEmpty()) {
                                        currentRuns.add(
                                            RichRun(
                                                text = runTextBuilder.toString(),
                                                fontSizeSp = currentFontSizeSp,
                                                colorHex = currentColorHex,
                                                fontFamily = currentFontFamily,
                                                isBold = currentIsBold,
                                                isItalic = currentIsItalic
                                            )
                                        )
                                        runTextBuilder = StringBuilder()
                                    }
                                    currentRuns.add(
                                        RichRun(
                                            text = "\n",
                                            fontSizeSp = currentFontSizeSp,
                                            colorHex = currentColorHex
                                        )
                                    )
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when (name) {
                            "solidFill" -> inSolidFill = false
                            "rPr" -> inRunProps = false
                            "r" -> {
                                if (inRun) {
                                    val text = runTextBuilder.toString()
                                    if (text.isNotEmpty()) {
                                        currentRuns.add(
                                            RichRun(
                                                text = text,
                                                fontSizeSp = currentFontSizeSp,
                                                colorHex = currentColorHex,
                                                fontFamily = currentFontFamily,
                                                isBold = currentIsBold,
                                                isItalic = currentIsItalic
                                            )
                                        )
                                    }
                                    inRun = false
                                }
                            }
                            "p" -> {
                                if (inParagraph) {
                                    if (currentRuns.isNotEmpty()) {
                                        paragraphs.add(
                                            RichParagraph(
                                                alignment = currentAlignment,
                                                runs = currentRuns
                                            )
                                        )
                                    }
                                    inParagraph = false
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing slide XML", e)
        }

        return paragraphs
    }

    private fun mapSchemeColor(scheme: String): String {
        return when (scheme.lowercase()) {
            "lt1", "lt2", "bg1", "bg2" -> "#FFFFFF"
            "accent1" -> "#FFD700" // Stage Gold
            "accent2" -> "#FF6B6B" // Coral Red
            "accent3" -> "#4ECDC4" // Cyan
            "accent4" -> "#45B7D1" // Sky Blue
            "accent5" -> "#96CEB4" // Mint
            "accent6" -> "#FFEEAD" // Soft Cream
            "dk1", "dk2", "tx1", "tx2" -> "#E0E0E0" // Light gray on stage
            else -> "#FFFFFF"
        }
    }
}
