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
import org.wysko.midis2jam2.world.lyric.LyricController
import org.wysko.midis2jam2.world.lyric.LyricLine
import org.wysko.midis2jam2.world.lyric.renderString
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    @Spec("lyrics.upcoming-line")
    fun `the next line is shown smaller beneath the current one`() {
        HeadlessPerformance.start(MidiFixtures.withLyrics()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )
            val (first, second) = performance.onEngineThread { controller.allLines }
            val texts = performance.onEngineThread { lyricTextsByLine(performance, controller) }

            // Partway through the first line, with the display settled.
            performance.onEngineThread { playUntil(controller, 2.5.seconds) }
            performance.onEngineThread {
                assertEquals(first, controller.currentLine)
                assertEquals(second, controller.upcomingLine, "The next line should be shown beneath the current one")
                assertTrue(
                    texts.getValue(second).color.a > 0.9f,
                    "The next line is meant to be visible beneath the current one, but it is hidden"
                )
                assertTrue(
                    controller.topOf(second) < controller.topOf(first),
                    "The next line should sit beneath the current one"
                )
                assertTrue(
                    texts.getValue(second).size < texts.getValue(first).size,
                    "The next line should be smaller than the current one"
                )
            }

            // Into the last line, which has nothing after it.
            performance.onEngineThread { playUntil(controller, 6.seconds) }
            performance.onEngineThread {
                assertEquals(second, controller.currentLine)
                assertNull(controller.upcomingLine, "The last line has no next line to show")
                assertTrue(
                    texts.getValue(second).size > texts.getValue(first).size * 0.99f,
                    "The next line should grow to full size once it is being sung"
                )
            }
        }
    }

    @Test
    @Spec("lyrics.upcoming-line.not-across-break")
    fun `the next line is not shown when a break between stanzas comes first`() {
        HeadlessPerformance.start(MidiFixtures.withStanzaBreak()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )
            val (first, second) = performance.onEngineThread { controller.allLines }
            val texts = performance.onEngineThread { lyricTextsByLine(performance, controller) }

            performance.onEngineThread { playUntil(controller, 2.5.seconds) }
            performance.onEngineThread {
                assertEquals(first, controller.currentLine)
                assertNull(controller.upcomingLine, "A line after a break between stanzas shouldn't be shown early")
                assertTrue(
                    texts.getValue(second).color.a < 0.05f,
                    "The line after the break is visible beneath the current line"
                )
            }
        }
    }

    /**
     * After a break between stanzas, the display fades back in. It used to fade in while still on the line
     * before the break, and only then switch, so that old line flashed up again for a moment.
     */
    @Test
    fun `after a break between stanzas the display comes back on the new line`() {
        HeadlessPerformance.start(MidiFixtures.withStanzaBreak()).use { performance ->
            val controller = assertNotNull(
                performance.app.stateManager.getState(LyricManager::class.java)?.controller
            )
            val (first, second) = performance.onEngineThread { controller.allLines }
            val texts = performance.onEngineThread { lyricTextsByLine(performance, controller) }

            // From well into the break, once the display has faded out, through the second stanza's entrance.
            val firstLineAlpha = performance.onEngineThread {
                buildList {
                    playUntil(controller, 15.seconds) { at -> if (at > 10.seconds) add(texts.getValue(first).color.a) }
                }
            }
            assertTrue(
                firstLineAlpha.all { it < 0.01f },
                "The line before the break reappeared as the display faded back in: max alpha ${firstLineAlpha.max()}"
            )
            performance.onEngineThread {
                assertEquals(second, controller.currentLine)
                assertTrue(texts.getValue(second).color.a > 0.9f, "The new stanza's line isn't showing")
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

        /**
         * Plays the lyric display from the start until [end], a frame at a time. Call it in one engine task:
         * the manager's own updates tick the display at playback time, which the test doesn't advance.
         */
        fun playUntil(
            controller: LyricController,
            end: Duration,
            eachFrame: (at: Duration) -> Unit = {},
        ) {
            var at = Duration.ZERO
            while (at < end) {
                at += HeadlessPerformance.FRAME
                controller.tick(at, HeadlessPerformance.FRAME)
                eachFrame(at)
            }
        }

        /** Each line's text on screen. */
        fun lyricTextsByLine(performance: HeadlessPerformance, controller: LyricController): Map<LyricLine, BitmapText> {
            val texts = bitmapTextsIn(performance.app.guiNode)
            return controller.allLines.associateWith { line -> texts.first { it.text == line.renderString() } }
        }

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
