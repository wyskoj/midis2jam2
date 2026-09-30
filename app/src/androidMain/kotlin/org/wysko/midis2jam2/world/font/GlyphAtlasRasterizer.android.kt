/*
 * Copyright (C) 2026 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.world.font

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.jme3.asset.AssetKey
import com.jme3.asset.AssetManager
import com.jme3.texture.Image
import com.jme3.texture.image.ColorSpace
import com.jme3.util.BufferUtils
import java.io.File

/** Maximum atlas row width; glyphs are shelf-packed within it, growing the atlas downward. */
private const val ATLAS_WIDTH = 512
private const val GLYPH_PADDING = 1

internal actual fun rasterizeGlyphAtlas(
    assetManager: AssetManager,
    chars: Set<Char>,
    basePixelSize: Int,
): GlyphAtlasData {
    val interBytes = assetManager
        .locateAsset(AssetKey<Any>("Assets/Fonts/Inter.ttf"))!!
        .openStream()
        .use { it.readBytes() }
    val interFile = File.createTempFile("midis2jam2-inter", ".ttf").apply {
        writeBytes(interBytes)
        deleteOnExit()
    }
    val interTypeface = Typeface.createFromFile(interFile)

    // Android's Paint does automatic system-font glyph fallback for characters the set Typeface
    // doesn't cover, so — unlike desktop's AWT — no manual per-character font search is needed.
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = interTypeface
        textSize = basePixelSize.toFloat()
        color = Color.WHITE
    }

    val fontMetrics = paint.fontMetricsInt
    val ascent = -fontMetrics.ascent
    val descent = fontMetrics.descent

    data class Placed(val char: Char, val bounds: Rect, val advance: Int)

    val measureBounds = Rect()
    val placements = chars.map { char ->
        val text = char.toString()
        paint.getTextBounds(text, 0, 1, measureBounds)
        val advance = paint.measureText(text).let { kotlin.math.round(it).toInt() }
        Placed(char, Rect(measureBounds), advance)
    }

    var cursorX = GLYPH_PADDING
    var cursorY = GLYPH_PADDING
    var shelfHeight = 0
    val positions = mutableMapOf<Char, Pair<Int, Int>>()
    for (placed in placements) {
        val width = placed.bounds.width()
        val height = placed.bounds.height()
        if (width <= 0 || height <= 0) {
            positions[placed.char] = 0 to 0
            continue
        }
        if (cursorX + width + GLYPH_PADDING > ATLAS_WIDTH) {
            cursorX = GLYPH_PADDING
            cursorY += shelfHeight + GLYPH_PADDING
            shelfHeight = 0
        }
        positions[placed.char] = cursorX to cursorY
        cursorX += width + GLYPH_PADDING
        shelfHeight = maxOf(shelfHeight, height)
    }
    val atlasHeight = (cursorY + shelfHeight + GLYPH_PADDING).coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(ATLAS_WIDTH, atlasHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val glyphs = mutableMapOf<Char, GlyphMetrics>()
    for (placed in placements) {
        val (posX, posY) = positions[placed.char]!!
        val width = placed.bounds.width().coerceAtLeast(0)
        val height = placed.bounds.height().coerceAtLeast(0)
        if (width > 0 && height > 0) {
            // placed.bounds is relative to the pen origin (0, 0) at the baseline; left/top are the
            // ink's offset from that origin.
            canvas.drawText(
                placed.char.toString(),
                (posX - placed.bounds.left).toFloat(),
                (posY - placed.bounds.top).toFloat(),
                paint,
            )
        }
        glyphs[placed.char] = GlyphMetrics(
            x = posX,
            y = posY,
            width = width,
            height = height,
            xOffset = placed.bounds.left,
            yOffset = ascent + placed.bounds.top,
            xAdvance = placed.advance,
        )
    }

    return GlyphAtlasData(
        image = bitmapToImage(bitmap, flipY = true),
        atlasWidth = ATLAS_WIDTH,
        atlasHeight = atlasHeight,
        lineHeight = ascent + descent,
        base = ascent,
        renderedSize = basePixelSize,
        glyphs = glyphs,
    )
}

private fun bitmapToImage(bitmap: Bitmap, flipY: Boolean): Image {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val data = BufferUtils.createByteBuffer(width * height * 4)
    for (y in 0 until height) {
        val sourceY = if (flipY) height - y - 1 else y
        for (x in 0 until width) {
            val argb = pixels[sourceY * width + x]
            val alpha = ((argb ushr 24) and 0xFF).toByte()
            val red = ((argb ushr 16) and 0xFF).toByte()
            val green = ((argb ushr 8) and 0xFF).toByte()
            val blue = (argb and 0xFF).toByte()
            data.put(red).put(green).put(blue).put(alpha)
        }
    }
    data.flip()

    return Image(Image.Format.RGBA8, width, height, data, null, ColorSpace.sRGB)
}
