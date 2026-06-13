package com.example.sketchbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.InputStream

class SketchbookRenderer(private val context: Context) {

    data class TextSegment(val text: String, val color: Int)

    fun render(
        text: String,
        emotion: String,
        config: AppConfig = AppConfig(),
    ): Bitmap? {
        val inputText = text.ifEmpty { return null }

        val baseBitmap = loadBaseImage(emotion) ?: return null
        val result = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val typeface = loadTypeface()

        val x1 = config.textBoxLeft
        val y1 = config.textBoxTop
        val x2 = config.textBoxRight
        val y2 = config.textBoxBottom
        val regionW = x2 - x1
        val regionH = y2 - y1
        if (regionW <= 0 || regionH <= 0) return result

        val optimalSize = findOptimalFontSize(inputText, typeface, regionW, regionH, config)
        if (optimalSize <= 0) return result

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = optimalSize.toFloat()
            color = Color.BLACK
        }

        val lines = wrapLines(inputText, paint, regionW)
        if (lines.isEmpty()) return result

        val fm = paint.fontMetrics
        val rawLineH = (fm.descent - fm.ascent).toDouble()
        val lineHeight = (rawLineH * (1.0 + config.lineSpacing)).toInt()
        val blockHeight = lineHeight * lines.size

        val yStart = when (config.valign) {
            "top" -> y1
            "middle" -> y1 + (regionH - blockHeight) / 2
            else -> y2 - blockHeight
        }.coerceAtLeast(y1)

        var inBracket = false
        var baselineY = yStart - fm.ascent

        for (line in lines) {
            val lineWidth = paint.measureText(line).toInt()
            val startX = when (config.align) {
                "left" -> x1
                "center" -> x1 + (regionW - lineWidth) / 2
                else -> x2 - lineWidth
            }

            val (segments, nextBracket) = parseColorSegments(line, inBracket)
            inBracket = nextBracket

            var drawX = startX.toFloat()
            for ((segText, segColor) in segments) {
                paint.color = segColor
                canvas.drawText(segText, drawX, baselineY.toFloat(), paint)
                drawX += paint.measureText(segText)
            }
            baselineY += lineHeight
            if (baselineY > y2.toFloat()) break
        }

        if (config.useOverlay) {
            loadOverlay()?.let { overlay ->
                canvas.drawBitmap(overlay, 0f, 0f, null)
            }
        }

        return result
    }

    private fun loadBaseImage(emotion: String): Bitmap? {
        val path = "BaseImages/$emotion.png"
        return try {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            try {
                context.assets.open("BaseImages/base.png").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun loadOverlay(): Bitmap? {
        return try {
            context.assets.open("BaseImages/base_overlay.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadTypeface(): Typeface {
        return try {
            Typeface.createFromAsset(context.assets, "fonts/font.ttf")
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun wrapLines(text: String, paint: Paint, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        for (para in text.split("\n")) {
            if (para.isEmpty()) {
                lines.add("")
                continue
            }

            val hasSpace = para.contains(" ")
            val units = if (hasSpace) para.split(" ") else para.map { it.toString() }

            val buf = StringBuilder()
            fun join(a: String, b: String): String =
                if (a.isEmpty()) b else if (hasSpace) "$a $b" else "$a$b"

            for (unit in units) {
                val trial = join(buf.toString(), unit)
                val w = paint.measureText(trial)

                if (w <= maxWidth) {
                    buf.clear(); buf.append(trial)
                } else {
                    if (buf.isNotEmpty()) {
                        lines.add(buf.toString()); buf.clear()
                    }
                    if (hasSpace && unit.length > 1) {
                        var tmp = ""
                        for (ch in unit) {
                            val t = tmp + ch
                            if (paint.measureText(t) <= maxWidth) {
                                tmp = t
                            } else {
                                if (tmp.isNotEmpty()) { lines.add(tmp); tmp = "" }
                                tmp = ch.toString()
                            }
                        }
                        if (tmp.isNotEmpty()) buf.append(tmp)
                    } else {
                        if (paint.measureText(unit) <= maxWidth) {
                            buf.append(unit)
                        } else {
                            lines.add(unit)
                        }
                    }
                }
            }
            if (buf.isNotEmpty()) lines.add(buf.toString())
            if (para.isEmpty() && (lines.isEmpty() || lines.last() != "")) lines.add("")
        }
        return lines
    }

    private fun findOptimalFontSize(
        text: String, typeface: Typeface, maxWidth: Int, maxHeight: Int, config: AppConfig
    ): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface }
        var lo = 1
        var hi = maxHeight
        var bestSize = 0

        while (lo <= hi) {
            val mid = (lo + hi) / 2
            paint.textSize = mid.toFloat()
            val lines = wrapLines(text, paint, maxWidth)
            if (lines.isEmpty()) { lo = mid + 1; continue }

            val fm = paint.fontMetrics
            val rawH = (fm.descent - fm.ascent).toDouble()
            val lineH = (rawH * (1.0 + config.lineSpacing)).toInt()
            val totalH = lineH * lines.size
            val maxLineW = lines.maxOfOrNull { paint.measureText(it).toInt() } ?: 0

            if (maxLineW <= maxWidth && totalH <= maxHeight) {
                bestSize = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return bestSize.coerceAtLeast(1)
    }

    companion object {
        private val BRACKET_COLOR = Color.rgb(128, 0, 128)

        fun parseColorSegments(line: String, inBracket: Boolean): Pair<List<TextSegment>, Boolean> {
            val segments = mutableListOf<TextSegment>()
            var bracket = inBracket
            val buf = StringBuilder()

            for (ch in line) {
                when {
                    ch == '[' || ch == '【' -> {
                        if (buf.isNotEmpty()) {
                            segments.add(TextSegment(buf.toString(), if (bracket) BRACKET_COLOR else Color.BLACK))
                            buf.clear()
                        }
                        segments.add(TextSegment(ch.toString(), BRACKET_COLOR))
                        bracket = true
                    }
                    ch == ']' || ch == '】' -> {
                        if (buf.isNotEmpty()) {
                            segments.add(TextSegment(buf.toString(), BRACKET_COLOR))
                            buf.clear()
                        }
                        segments.add(TextSegment(ch.toString(), BRACKET_COLOR))
                        bracket = false
                    }
                    else -> buf.append(ch)
                }
            }
            if (buf.isNotEmpty()) {
                segments.add(TextSegment(buf.toString(), if (bracket) BRACKET_COLOR else Color.BLACK))
            }
            return Pair(segments, bracket)
        }
    }
}
