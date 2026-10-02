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

import com.jme3.texture.Image

/**
 * The raster placement and metrics of a single glyph within a [GlyphAtlasData]'s texture, in the
 * same terms as a single `char` line of the AngelCode BMFont format (see `Assets/Fonts/Inter.fnt`).
 */
internal data class GlyphMetrics(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val xOffset: Int,
    val yOffset: Int,
    val xAdvance: Int,
)

/**
 * A rasterized glyph atlas: one texture image plus per-character placement/metrics, mirroring the
 * fields of an AngelCode BMFont `info`/`common`/`char` block closely enough that it can be turned
 * into a jME [com.jme3.font.BitmapFont] the same way a `.fnt` file is (see [buildBitmapFont]).
 *
 * @property renderedSize The pixel size the glyphs were rasterized at (the `.fnt` format's `info size`).
 */
internal data class GlyphAtlasData(
    val image: Image,
    val atlasWidth: Int,
    val atlasHeight: Int,
    val lineHeight: Int,
    val base: Int,
    val renderedSize: Int,
    val glyphs: Map<Char, GlyphMetrics>,
)
