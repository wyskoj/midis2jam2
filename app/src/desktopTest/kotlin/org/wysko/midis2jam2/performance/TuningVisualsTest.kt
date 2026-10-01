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

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.TuningMotion
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * How a guitar shows its tuning and capo in a real (headless) performance: a retuned string rings and glides into its
 * new tension before the first note, slack strings vibrate wider, and a capo sits on its fret across the neck.
 */
class TuningVisualsTest {

    @Test
    @Spec("instrument.fretted.tuning.retune", "instrument.fretted.tuning.slack")
    fun `a drop-D guitar rings its low string into tune before it plays, and the low string stays slack`() {
        HeadlessPerformance.start(MidiFixtures.dropDRiff(delayed = true), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            assertEquals(listOf(-2, 0, 0, 0, 0, 0), guitar.fretting.tuning.let { t -> (0 until 6).map { t[it] - STANDARD[it] } })

            stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS - (TuningMotion.RETUNE_LEAD + TuningMotion.RETUNE_GAP) / 2)
            val ringing = performance.onEngineThread { visibleStringFrames(guitar.geometry) }
            assertEquals(
                listOf("GuitarLowStringBottom"),
                ringing.map { it.removeSuffix(".obj").dropLastWhile(Char::isDigit).substringAfterLast('/') }.distinct(),
                "Only the low string should ring while it is tuned; ringing: $ringing",
            )
            assertEquals(1, ringing.size, "One frame of one string's vibration should show: $ringing")

            stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS + 0.5)
            performance.onEngineThread {
                assertTrue(guitar.tuning.vibrationWidth(0) > 1.0, "The slack low string should swing wider")
                assertTrue(guitar.tuning.vibrationSpeed(0) < 1.0, "The slack low string should vibrate slower")
                (1 until 6).forEach { assertEquals(1.0, guitar.tuning.vibrationWidth(it), "String $it is in standard tuning") }
            }
        }
    }

    @Test
    @Spec("instrument.fretted.tuning.slack")
    fun `a guitar in standard tuning keeps its usual strings and no capo`() {
        HeadlessPerformance.start(MidiFixtures.humanizedGuitarChords(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            performance.stepInstruments(frames = 30)
            performance.onEngineThread {
                assertTrue(!guitar.tuning.motion.isRetuned)
                assertNull(guitar.tuning.capo, "Standard chords need no capo")
                (0 until 6).forEach {
                    assertEquals(1.0, guitar.tuning.vibrationSpeed(it))
                    assertEquals(1.0, guitar.tuning.vibrationWidth(it))
                }
            }
        }
    }

    @Test
    @Spec("instrument.fretted.capo")
    fun `a capo is clamped across the neck just behind its fret`() {
        HeadlessPerformance.start(MidiFixtures.capoChords(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            assertEquals(2, guitar.fretting.capo)
            // The visuals are built on the engine thread by the first frame.
            performance.stepInstruments(frames = 1)
            val capo = performance.onEngineThread { assertNotNull(guitar.tuning.capo, "A part played with a capo should show one") }
            performance.onEngineThread { assertEquals(Spatial.CullHint.Always, capo.cullHint, "No capo before the retune") }

            stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS + 0.5)
            performance.onEngineThread {
                assertTrue(capo.cullHint != Spatial.CullHint.Always, "The capo should be on the neck")
                val board = guitar.fretboard
                val middle = (board.stringCount - 1) / 2.0
                fun along(fret: Double) = board.pointOn(middle, fret).dot(board.along)
                val at = capo.localTranslation.dot(board.along)
                assertTrue(at > along(1.0) && at < along(2.0), "The capo should sit between frets 1 and 2")
                val neck = board.pointOn(0.0, 1.75).distance(board.pointOn(board.stringCount - 1.0, 1.75))
                assertTrue(capo.localScale.x > neck, "The capo should span every string")
            }

            // Seeking back before the retune takes the capo off again.
            performance.stepInstruments(frames = 1, delta = (-2.5).seconds)
            performance.onEngineThread { assertEquals(Spatial.CullHint.Always, capo.cullHint) }
        }
    }

    private fun stepTo(performance: HeadlessPerformance, seconds: Double) {
        val frames = ((seconds.seconds - performance.time) / HeadlessPerformance.FRAME).roundToInt()
        performance.stepInstruments(frames = frames)
    }

    /** The model names of the vibrating lower-string frames currently shown under [node]. */
    private fun visibleStringFrames(node: Node): List<String> = buildList {
        fun visit(spatial: Spatial) {
            if (spatial.cullHint == Spatial.CullHint.Always) return
            spatial.key?.name?.takeIf { "StringBottom" in it }?.let { add(it) }
            if (spatial is Node) spatial.children.forEach(::visit)
        }
        visit(node)
    }

    private companion object {
        val STANDARD = intArrayOf(40, 45, 50, 55, 59, 64)
    }
}
