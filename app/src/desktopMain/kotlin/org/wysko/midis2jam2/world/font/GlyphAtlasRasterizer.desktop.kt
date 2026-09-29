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

import com.jme3.asset.AssetKey
import com.jme3.asset.AssetManager
import com.jme3.texture.plugins.AWTLoader
import java.awt.Font
import java.awt.Font.TRUETYPE_FONT
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    val interFont = Font.createFont(TRUETYPE_FONT, interBytes.inputStream()).deriveFont(basePixelSize.toFloat())

    val frc = FontRenderContext(null, true, true)
    val lineMetrics = interFont.getLineMetrics("Hg", frc)
    val ascent = ceil(lineMetrics.ascent).toInt()
    val descent = ceil(lineMetrics.descent).toInt()

    val fallbackCache = mutableMapOf<Char, Font>()
    fun fontFor(char: Char): Font {
        if (interFont.canDisplay(char)) return interFont
        fallbackCache[char]?.let { return it }
        val sansSerif = Font(Font.SANS_SERIF, Font.PLAIN, basePixelSize)
        val resolved = when {
            sansSerif.canDisplay(char) -> sansSerif
            else -> GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
                .firstOrNull { it.canDisplay(char) }
                ?.deriveFont(basePixelSize.toFloat())
                ?: sansSerif
        }
        fallbackCache[char] = resolved
        return resolved
    }

    data class Placed(val char: Char, val font: Font, val bounds: java.awt.Rectangle, val advance: Int)

    val placements = chars.map { char ->
        val font = fontFor(char)
        val glyphVector = font.createGlyphVector(frc, char.toString())
        val bounds = glyphVector.getPixelBounds(frc, 0f, 0f)
        val advance = glyphVector.getGlyphMetrics(0).advanceX.roundToInt()
        Placed(char, font, bounds, advance)
    }

    var cursorX = GLYPH_PADDING
    var cursorY = GLYPH_PADDING
    var shelfHeight = 0
    val positions = mutableMapOf<Char, Pair<Int, Int>>()
    for (placed in placements) {
        val width = placed.bounds.width.coerceAtLeast(0)
        val height = placed.bounds.height.coerceAtLeast(0)
        if (width == 0 || height == 0) {
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

    val image = BufferedImage(ATLAS_WIDTH, atlasHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        color = java.awt.Color.WHITE
    }

    val glyphs = mutableMapOf<Char, GlyphMetrics>()
    for (placed in placements) {
        val (posX, posY) = positions[placed.char]!!
        val width = placed.bounds.width.coerceAtLeast(0)
        val height = placed.bounds.height.coerceAtLeast(0)
        if (width > 0 && height > 0) {
            graphics.font = placed.font
            graphics.drawString(
                placed.char.toString(),
                (posX - placed.bounds.x).toFloat(),
                (posY - placed.bounds.y).toFloat(),
            )
        }
        glyphs[placed.char] = GlyphMetrics(
            x = posX,
            y = posY,
            width = width,
            height = height,
            xOffset = placed.bounds.x,
            yOffset = ascent + placed.bounds.y,
            xAdvance = placed.advance,
        )
    }
    graphics.dispose()

    return GlyphAtlasData(
        image = AWTLoader().load(image, true),
        atlasWidth = ATLAS_WIDTH,
        atlasHeight = atlasHeight,
        lineHeight = ascent + descent,
        base = ascent,
        renderedSize = basePixelSize,
        glyphs = glyphs,
    )
}
