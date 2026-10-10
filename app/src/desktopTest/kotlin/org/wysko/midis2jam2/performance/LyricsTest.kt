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

package org.wysko.midis2jam2.performance

import com.jme3.font.BitmapText
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.OnScreenElementsSettings.LyricsSettings.LyricsStyle
import org.wysko.midis2jam2.manager.LyricManager
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.testing.withLyrics
import org.wysko.midis2jam2.world.font.covers
import org.wysko.midis2jam2.world.lyric.renderString
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The lyric display, and the reason this suite exists.
 *
 * The lyric controller was fully written and then quietly stopped being constructed during a
 * refactor. Nothing failed to compile, the setting stayed in the settings screen, the
 * documentation went on promising the feature, and the only way to notice was to play a file
 * with lyrics and look. These tests watch the wiring, not just the text handling, so that it
 * cannot come loose again unnoticed.
 */
class LyricsTest {

    @Test
    @Spec("lyrics.displayed-when-present")
    fun `a file with lyrics gets a lyric display`() {
        HeadlessPerformance.start(MidiFixtures.withLyrics()).use { performance ->
            val manager = performance.app.stateManager.getState(LyricManager::class.java)

            assertNotNull(
                manager,
                "The file contains lyrics and the setting is on, but no lyric display was " +
                    "attached to the performance"
            )
            assertNotNull(manager.controller, "The lyric display was attached but never initialised")
        }
    }

    @Test
    fun `a file without lyrics gets no lyric display`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            assertNull(
                performance.app.stateManager.getState(LyricManager::class.java),
                "A file with no lyrics should not pay for a lyric display"
            )
        }
    }

    @Test
    @Spec("lyrics.setting.toggle")
    fun `turning lyrics off in the settings removes the display`() {
        val lyricsOff = AppSettings().withLyrics { copy(isShowLyrics = false) }

        HeadlessPerformance.start(MidiFixtures.withLyrics(), settings = lyricsOff).use { performance ->
            assertNull(
                performance.app.stateManager.getState(LyricManager::class.java),
                "Lyrics are turned off, but a lyric display was attached anyway"
            )
        }
    }

    @Test
    @Spec("lyrics.setting.size")
    fun `the lyric size setting is carried into the performance`() {
        val large = AppSettings().withLyrics { copy(lyricsSize = 3.0) }

        HeadlessPerformance.start(MidiFixtures.withLyrics(), settings = large).use { performance ->
            // The controller reads the size while building its text, so reaching this point
            // with a display attached means the setting was applied rather than ignored.
            val manager = performance.app.stateManager.getState(LyricManager::class.java)
            assertNotNull(manager?.controller, "No lyric display was built at a non-default size")

            val configured = performance.performance.config
                .settings
                .onScreenElementsSettings
                .lyricsSettings
                .lyricsSize
            assertEquals(3.0, configured)
        }
    }

    @Test
    @Spec("lyrics.current-line-first")
    fun `the lyrics are split into the lines they were written as`() {
        HeadlessPerformance.start(MidiFixtures.withLyrics()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )

            val lines = performance.onEngineThread { controller.allLines.map { it.renderString() } }

            assertEquals(
                listOf("Twinkle twinkle little star", "How I wonder what you are"),
                lines,
                "The lyric line break was not honoured"
            )
        }
    }

    @Test
    @Spec("lyrics.elapsed-syllables-white", "lyrics.upcoming-syllables-gray")
    fun `syllables elapse as the song plays`() {
        HeadlessPerformance.start(MidiFixtures.withLyrics()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )

            // Nothing has been sung yet, so nothing is highlighted.
            val atStart = performance.onEngineThread { controller.elapsedCharactersOfCurrentLine }
            assertEquals(0, atStart, "Syllables were already highlighted before the song began")

            // Drive the display forward the way the performance does.
            val elapsedOverTime = mutableListOf<Int>()
            repeat(STEPS) { step ->
                val at = HeadlessPerformance.FRAME * ((step + 1) * FRAMES_PER_STEP)
                performance.onEngineThread { controller.tick(at, HeadlessPerformance.FRAME) }
                elapsedOverTime += performance.onEngineThread { controller.elapsedCharactersOfCurrentLine }
            }

            assertTrue(
                elapsedOverTime.any { it > 0 },
                "No syllable was ever highlighted while the song played: $elapsedOverTime"
            )
            assertTrue(
                elapsedOverTime.zipWithNext().all { (before, after) -> after >= before || after == 0 },
                "The highlight went backwards within a line: $elapsedOverTime"
            )
            assertTrue(
                performance.onEngineThread { controller.currentLine != null },
                "The display never settled on a current line"
            )
        }
    }

    @Test
    @Spec("lyrics.style.glide")
    fun `in the glide style the highlight sweeps through each syllable`() {
        val glide = AppSettings().withLyrics { copy(style = LyricsStyle.Glide) }

        HeadlessPerformance.start(MidiFixtures.withLyrics(), settings = glide).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )

            val lyricTexts = performance.onEngineThread { bitmapTextsIn(performance.app.guiNode) }
                .filter { text -> controller.allLines.any { it.renderString() == text.text } }
            assertTrue(lyricTexts.isNotEmpty(), "No lyric line text was found on screen")
            lyricTexts.flatMap { it.children.filterIsInstance<Geometry>() }.forEach {
                assertEquals(
                    "LyricWipe",
                    it.material.materialDef.name,
                    "A lyric line is drawn without the wipe the glide style needs"
                )
            }

            val sungOverTime = mutableListOf<Pair<String?, Float>>()
            repeat(STEPS) { step ->
                val at = HeadlessPerformance.FRAME * ((step + 1) * FRAMES_PER_STEP)
                // Read in the same engine task as the tick: the manager's own updates tick at playback time.
                sungOverTime += performance.onEngineThread {
                    controller.tick(at, HeadlessPerformance.FRAME)
                    controller.currentLine?.renderString() to controller.sungCharactersOfCurrentLine
                }
            }

            val sung = sungOverTime.map { it.second }
            assertTrue(
                sung.any { it != floor(it) },
                "The highlight only ever covered whole characters, so it jumped instead of gliding: $sung"
            )
            sungOverTime.zipWithNext().filter { (before, after) -> before.first == after.first }
                .forEach { (before, after) ->
                    assertTrue(
                        after.second >= before.second,
                        "The highlight went backwards within a line: $sungOverTime"
                    )
                }
            sungOverTime.groupBy({ it.first }, { it.second }).forEach { (line, values) ->
                assertTrue(
                    values.zipWithNext().all { (before, after) -> after - before <= MAX_STEP_CHARACTERS },
                    "The highlight leapt through \"$line\" rather than gliding: $values"
                )
            }
        }
    }

    @Test
    @Spec("lyrics.font.non-ascii-glyphs")
    fun `lyrics using characters outside the bundled font are still rendered`() {
        HeadlessPerformance.start(MidiFixtures.withNonAsciiLyrics()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )

            val lines = performance.onEngineThread { controller.allLines.map { it.renderString() } }
            val requiredChars = lines.flatMap { it.toSet() }.toSet()
            assertTrue(
                requiredChars.any { it.code > MAX_ASCII_CODE_POINT },
                "This fixture should require characters outside the bundled font's ASCII coverage"
            )

            val lyricTexts = performance.onEngineThread { bitmapTextsIn(performance.app.guiNode) }
                .filter { text -> lines.contains(text.text.toString()) }

            assertTrue(lyricTexts.isNotEmpty(), "No lyric line text was found on screen")
            lyricTexts.forEach { text ->
                assertTrue(
                    text.font.covers(requiredChars),
                    "The lyric display's font is missing glyphs used by the lyrics: ${text.text}"
                )
            }
        }
    }

    private companion object {
        const val STEPS = 40
        const val FRAMES_PER_STEP = 15

        /**
         * The most characters the glide may cover between two steps. A quarter note of the fixture is
         * 0.5 s and holds one syllable of at most four characters; a step is a quarter of a second, so a
         * glide covers about two characters a step, and an instant jump covers a whole syllable at once.
         */
        const val MAX_STEP_CHARACTERS = 3f

        /** The highest ASCII code point; anything above it is outside the bundled font's coverage. */
        const val MAX_ASCII_CODE_POINT = 126

        /** Every BitmapText currently in the scene under [root]. */
        fun bitmapTextsIn(root: Spatial): List<BitmapText> = buildList {
            fun visit(spatial: Spatial) {
                if (spatial is BitmapText) add(spatial)
                if (spatial is Node) spatial.children.forEach(::visit)
            }
            visit(root)
        }
    }
}
