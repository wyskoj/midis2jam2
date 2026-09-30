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

import com.jme3.asset.AssetManager
import com.jme3.font.BitmapCharacter
import com.jme3.font.BitmapCharacterSet
import com.jme3.font.BitmapFont
import com.jme3.material.Material
import com.jme3.material.RenderState.BlendMode
import com.jme3.texture.Texture
import com.jme3.texture.Texture2D

/**
 * Builds a jME [BitmapFont] from a rasterized [GlyphAtlasData], the same way jME's own
 * `.fnt`-format loader (`BitmapFontLoader`) builds one from a parsed text file — just reading
 * structured data directly instead of a file.
 */
internal fun buildBitmapFont(assetManager: AssetManager, atlas: GlyphAtlasData): BitmapFont {
    val charSet = BitmapCharacterSet().apply {
        renderedSize = atlas.renderedSize
        lineHeight = atlas.lineHeight
        base = atlas.base
        width = atlas.atlasWidth
        height = atlas.atlasHeight
    }

    val material = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
        setTexture(
            "ColorMap",
            Texture2D(atlas.image).apply {
                magFilter = Texture.MagFilter.Bilinear
                minFilter = Texture.MinFilter.BilinearNoMipMaps
            },
        )
        setBoolean("VertexColor", true)
        additionalRenderState.setBlendMode(BlendMode.Alpha)
    }

    atlas.glyphs.forEach { (char, metrics) ->
        charSet.addCharacter(
            char.code,
            BitmapCharacter(char).apply {
                x = metrics.x
                y = metrics.y
                width = metrics.width
                height = metrics.height
                xOffset = metrics.xOffset
                yOffset = metrics.yOffset
                xAdvance = metrics.xAdvance
                page = 0
            },
        )
    }

    return BitmapFont().apply {
        setCharSet(charSet)
        setPages(arrayOf(material))
    }
}

/**
 * Returns [staticFont] unchanged if it already has a glyph for every character in [chars] (the
 * common case — no behavior or performance change). Otherwise rasterizes and builds a fresh font
 * containing exactly those characters.
 */
internal fun resolveBitmapFont(
    assetManager: AssetManager,
    staticFont: BitmapFont,
    chars: Set<Char>,
    basePixelSize: Int,
): BitmapFont =
    if (staticFont.covers(chars)) {
        staticFont
    } else {
        buildBitmapFont(assetManager, rasterizeGlyphAtlas(assetManager, chars, basePixelSize))
    }
