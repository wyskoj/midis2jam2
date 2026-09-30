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

import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Internal regression coverage for the dynamic glyph atlas: the mechanism that lets lyrics and
 * the HUD file name render characters outside the bundled `.fnt` files' plain-ASCII coverage.
 * The user-facing behaviour this exists for is covered separately, with `@Spec`, in
 * `LyricsTest` and `OnScreenElementsTest`.
 */
class GlyphAtlasRasterizerTest {

    @Test
    fun `a rasterized atlas has a usable glyph for every requested character`() {
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            val chars = setOf('こ', 'ん', 'A', 'é')
            val font = performance.onEngineThread {
                val assetManager = performance.app.assetManager
                buildBitmapFont(assetManager, rasterizeGlyphAtlas(assetManager, chars, basePixelSize = 64))
            }

            assertTrue(font.covers(chars), "The dynamic atlas should have a glyph for every requested character")
            chars.forEach { char ->
                val glyph = assertNotNull(font.charSet.getCharacter(char.code), "Missing glyph for '$char'")
                assertTrue(
                    glyph.xAdvance > 0,
                    "The glyph for '$char' has no advance width, so text using it would collapse",
                )
            }
        }
    }

    @Test
    fun `resolving a font that already covers the requested text keeps the static font unchanged`() {
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            val assetManager = performance.app.assetManager
            val (staticFont, resolved) = performance.onEngineThread {
                val staticFont = assetManager.loadFont("Assets/Fonts/Inter.fnt")
                staticFont to resolveBitmapFont(assetManager, staticFont, setOf('a', 'b', 'c'), basePixelSize = 64)
            }

            assertSame(
                staticFont,
                resolved,
                "All-ASCII text is already covered by the bundled font, so no dynamic atlas should be built",
            )
        }
    }

    @Test
    fun `resolving a font missing glyphs builds a dynamic one that covers the missing characters`() {
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            val assetManager = performance.app.assetManager
            val (staticFont, resolved) = performance.onEngineThread {
                val staticFont = assetManager.loadFont("Assets/Fonts/Inter.fnt")
                staticFont to resolveBitmapFont(assetManager, staticFont, setOf('こ', 'ん'), basePixelSize = 64)
            }

            assertTrue(resolved !== staticFont, "Text outside the bundled font's coverage should get a dynamic atlas")
            assertTrue(resolved.covers(setOf('こ', 'ん')), "The dynamic font should cover the requested characters")
        }
    }
}
