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

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.TuningKeyLayout
import org.wysko.midis2jam2.instrument.family.guitar.TuningMotion
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * How a guitar shows its tuning and capo in a real (headless) performance: a retuned instrument is on stage, strings still,
 * while its keys turn before the first note, slack strings vibrate wider, and a capo sits on its fret across the neck.
 */
class TuningVisualsTest {

    @Test
    @Spec("instrument.fretted.tuning.retune", "instrument.fretted.tuning.slack")
    fun `a drop-D guitar is on stage with still strings while it retunes, and the low string stays slack`() {
        HeadlessPerformance.start(MidiFixtures.dropDRiff(delayed = true), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            assertEquals(listOf(-2, 0, 0, 0, 0, 0), guitar.fretting.tuning.let { t -> (0 until 6).map { t[it] - STANDARD[it] } })

            stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS - (TuningMotion.RETUNE_LEAD + TuningMotion.RETUNE_GAP) / 2)
            assertTrue(guitar.isVisible, "The guitar should be on stage to be seen retuning, though it plays nothing yet")
            val vibrating = performance.onEngineThread { visibleStringFrames(guitar.geometry) }
            assertTrue(vibrating.isEmpty(), "Only the keys move while tuning; the strings stay still: $vibrating")

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
    fun `a capo is clamped across the neck on its fret`() {
        HeadlessPerformance.start(MidiFixtures.capoChords(), attachManagers = false).use { performance ->
            assertCapoClamped(performance, performance.instruments.filterIsInstance<Guitar>().single())
        }
    }

    @Test
    @Spec("instrument.fretted.capo")
    fun `a banjo's capo is clamped on its fret`() {
        HeadlessPerformance.start(MidiFixtures.banjoCapoChords(), attachManagers = false).use { performance ->
            val banjo = performance.instruments.filterIsInstance<Banjo>().single()
            assertCapoClamped(performance, banjo)
        }
    }

    private fun assertCapoClamped(performance: HeadlessPerformance, guitar: FrettedInstrument, capoZ: Float = 0f) {
        assertEquals(2, guitar.fretting.capo)
        // The visuals are built on the engine thread by the first frame.
        stepTo(performance, BEFORE_RETUNE)
        val capo = performance.onEngineThread { assertNotNull(guitar.tuning.capo, "A part played with a capo should show one") }
        performance.onEngineThread { assertEquals(Spatial.CullHint.Always, capo.cullHint, "No capo before the retune") }

        stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS + 0.5)
        performance.onEngineThread {
            assertTrue(capo.cullHint != Spatial.CullHint.Always, "The capo should be on the neck")
            val board = guitar.fretboard
            val middle = (board.stringCount - 1) / 2.0
            fun along(fret: Double) = board.pointOn(middle, fret).dot(board.along)
            val at = capo.localTranslation.dot(board.along)
            assertEquals(along(2.0), at, 0.01f, "The capo should sit on its fret, over where the open strings start to vibrate")
            assertEquals(
                capoZ, capo.localTranslation.z, 1e-4f,
                "The capo should be clamped onto the strings by now",
            )
        }

        // Seeking back before the retune takes the capo off again.
        stepTo(performance, BEFORE_RETUNE)
        performance.onEngineThread { assertEquals(Spatial.CullHint.Always, capo.cullHint) }
    }

    @Test
    @Spec("instrument.fretted.tuning.keys")
    fun `a drop-D guitar turns its low key during the retune, and leaves the others`() {
        HeadlessPerformance.start(MidiFixtures.dropDRiff(delayed = true), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            stepTo(performance, BEFORE_RETUNE)
            val keys = performance.onEngineThread { guitar.tuning.keys }
            assertEquals(6, keys.size, "The guitar should have a key per string")
            val rest = performance.onEngineThread { keys.map { it.localRotation.clone() } }

            stepTo(performance, MidiFixtures.TUNED_PART_START_SECONDS + 0.5)
            performance.onEngineThread {
                val axis = Vector3f()
                val turn = rest[0].inverse().mult(keys[0].localRotation).toAngleAxis(axis) * FastMath.RAD_TO_DEG
                assertEquals(40f, turn, 0.5f, "Two semitones down should turn the low key 40 degrees")
                assertEquals(1f, abs(axis.y), 0.001f, "The key should turn about its own post")
                (1 until 6).forEach {
                    assertTrue(rest[it].isSimilar(keys[it].localRotation, 1e-4f), "Key $it's string stays in standard tuning")
                }
            }

            // Seeking back before the retune turns the key back.
            stepTo(performance, BEFORE_RETUNE)
            performance.onEngineThread { assertTrue(rest[0].isSimilar(keys[0].localRotation, 1e-4f)) }
        }
    }

    @Test
    fun `every instrument with key art has a key for each string`() {
        mapOf("Guitar" to 6, "GuitarAcoustic" to 6, "Bass" to 4, "BassFretless" to 4, "Banjo" to 4).forEach { (name, strings) ->
            val layout = assertNotNull(TuningKeyLayout.load(name), "$name should have key art")
            assertEquals(strings, layout.keys.size, "$name's keys")
        }
    }

    /**
     * Key poses are written as Blender shows them; they must be converted the same way Blender's OBJ exporter converts
     * the meshes (Y up), or the keys land off the headstock. The expected matrices were worked out independently, and
     * place the keys onto where the old models had them.
     */
    @Test
    fun `Blender poses are converted the way the OBJ exporter converts meshes`() {
        assertEquals(Vector3f(1f, 3f, -2f), TuningKeyLayout.blenderPosition(1f, 2f, 3f))
        assertMatrix(
            floatArrayOf(0.2823f, -0.9593f, -0.0094f, 0.937f, 0.2736f, 0.217f, -0.2056f, -0.0701f, 0.9761f),
            TuningKeyLayout.blenderRotation(-38.426f, -69.561f, 36.073f),
        )
        assertMatrix(
            floatArrayOf(0.196f, -0.9806f, -0.0004f, 0.9656f, 0.1929f, 0.1746f, -0.1711f, -0.0346f, 0.9846f),
            TuningKeyLayout.blenderRotation(47.848f, -74.919f, 41.131f).mult(TuningKeyLayout.blenderRotation(-90f, 0f, 0f)),
        )
    }

    private fun assertMatrix(expected: FloatArray, rotation: Quaternion) {
        val m = rotation.toRotationMatrix()
        (0 until 9).forEach { assertEquals(expected[it], m.get(it / 3, it % 3), 0.001f, "Element ${it / 3},${it % 3}") }
    }

    /** Steps forward frame by frame to [seconds], or jumps straight back to it (as a seek does). */
    private fun stepTo(performance: HeadlessPerformance, seconds: Double) {
        val gap = seconds.seconds - performance.time
        if (gap.isNegative()) return performance.stepInstruments(frames = 1, delta = gap)
        performance.stepInstruments(frames = (gap / HeadlessPerformance.FRAME).roundToInt())
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

        /** A time before the fixtures' retunes, which start [TuningMotion.RETUNE_LEAD] before their first notes. */
        const val BEFORE_RETUNE = -1.9
    }
}
