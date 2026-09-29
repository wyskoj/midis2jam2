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

import org.wysko.midis2jam2.testing.withLyrics
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.manager.LyricManager
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.world.lyric.renderString
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

    private companion object {
        const val STEPS = 40
        const val FRAMES_PER_STEP = 15
    }
}
