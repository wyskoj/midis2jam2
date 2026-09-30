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

/**
 * Rasterizes [chars] into a fresh glyph atlas at [basePixelSize], using the bundled Inter
 * typeface (`Assets/Fonts/Inter.ttf`) where it has a glyph, and falling back to a font the
 * platform provides (e.g. a system CJK font) per character where Inter doesn't.
 *
 * [basePixelSize] should match the base size of the static `.fnt` this atlas is standing in for
 * (64 for `Inter.fnt`, 24 for `Inter_24.fnt`), so text scaling behaves the same way it did with
 * the static font.
 */
internal expect fun rasterizeGlyphAtlas(
    assetManager: AssetManager,
    chars: Set<Char>,
    basePixelSize: Int,
): GlyphAtlasData
