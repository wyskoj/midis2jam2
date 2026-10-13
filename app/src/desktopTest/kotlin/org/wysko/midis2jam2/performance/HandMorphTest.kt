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

import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.testing.withInstruments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Plays a few notes on the instruments that finger with hands and samples the hands' morph weights: steady in a
 * note's shape while it sounds, partway between two shapes just before the next note, and exactly in the next shape
 * once it starts. Also checks that the ocarina, whose hand shapes can't be morphed between, still loads.
 */
class HandMorphTest {

    /** Program, first note (four rising notes from there, half a second each), and a name for messages. */
    private val handedInstruments = listOf(
        Triple(71, 69, "clarinet"),
        Triple(68, 69, "oboe"),
        Triple(73, 64, "flute"),
        Triple(72, 76, "piccolo"),
        Triple(74, 64, "recorder"),
    )

    @Test
    @Spec("instrument.hands.smooth-transitions")
    fun `the fingers move smoothly into each note's shape and arrive as it starts`() {
        for ((program, note, name) in handedInstruments) {
            HeadlessPerformance.start(MidiFixtures.singleProgram(program, note = note), attachManagers = false).use {
                val instrument = it.instruments.filterIsInstance<MonophonicInstrument>().single()
                val hands = it.onEngineThread { morphingHands(instrument) }
                assertEquals(2, hands.size, "The $name should have two morphing hands")

                it.stepInstruments(frames = 50, delta = STEP) // 0.25 s, mid first note
                val holding = it.onEngineThread { hands.map { hand -> hand.morphState.copyOf() } }

                it.stepInstruments(frames = 41, delta = STEP) // 0.455 s, mid move into the second note
                val moving = it.onEngineThread { hands.map { hand -> hand.morphState.copyOf() } }

                it.stepInstruments(frames = 10, delta = STEP) // 0.505 s, just after the second note starts
                val arrived = it.onEngineThread { hands.map { hand -> hand.morphState.copyOf() } }

                val expected = it.onEngineThread {
                    instrument.manager!!.fingering((note + 1).toByte()).toString()
                }
                arrived.zip(listOf("left", "right")).forEach { (weights, side) ->
                    assertTrue(
                        weights.all { w -> w == 0f || w == 1f } && weights.count { w -> w == 1f } <= 1,
                        "Once the second note starts, the $name's $side hand should be exactly in its shape " +
                            "($expected), but its weights are ${weights.toList()}"
                    )
                }

                val changing = holding.indices.filter { i -> !holding[i].contentEquals(arrived[i]) }
                assertTrue(changing.isNotEmpty(), "The $name's fixture should change hand shape between notes")
                changing.forEach { i ->
                    val between = moving[i].indices.all { w ->
                        val (a, b) = holding[i][w] to arrived[i][w]
                        moving[i][w] in minOf(a, b)..maxOf(a, b)
                    } && !moving[i].contentEquals(holding[i]) && !moving[i].contentEquals(arrived[i])
                    assertTrue(
                        between,
                        "Just before the second note, the $name's hand should be partway between " +
                            "${holding[i].toList()} and ${arrived[i].toList()}, but it's at ${moving[i].toList()}"
                    )
                }
                it.throwIfEngineFailed()
            }
        }
    }

    @Test
    @Spec("instrument.hands.smooth-transitions")
    fun `with smooth hand movement off, the hands snap between shapes`() {
        val snapping = AppSettings().withInstruments { copy(isSmoothHandMovement = false) }
        HeadlessPerformance.start(MidiFixtures.singleProgram(71, note = 69), snapping, attachManagers = false).use {
            val instrument = it.instruments.filterIsInstance<MonophonicInstrument>().single()
            it.stepInstruments(frames = 91, delta = STEP) // 0.455 s, where a smooth hand would be mid-move
            val (morphing, shown) = it.onEngineThread {
                morphingHands(instrument) to instrument.clones.first().geometry.children
                    .flatMap { node -> (node as? Node)?.children.orEmpty() }
                    .filter { shape -> shape.name.startsWith("Left") || shape.name.startsWith("Right") }
                    .count { shape -> shape.cullHint != Spatial.CullHint.Always }
            }
            assertTrue(morphing.isEmpty(), "With smooth hand movement off, the clarinet's hands shouldn't morph")
            assertEquals(2, shown, "With smooth hand movement off, exactly one shape per hand should be shown")
        }
    }

    @Test
    fun `the morphing hands cast and receive shadows like the rest of the instrument`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(71, note = 69), attachManagers = false).use {
            val instrument = it.instruments.filterIsInstance<MonophonicInstrument>().single()
            val modes = it.onEngineThread {
                instrument.geometry.shadowMode = RenderQueue.ShadowMode.CastAndReceive
                morphingHands(instrument).map { hand -> hand.shadowMode }
            }
            assertEquals(
                listOf(RenderQueue.ShadowMode.CastAndReceive, RenderQueue.ShadowMode.CastAndReceive),
                modes,
                "The hands should inherit the instrument's shadow mode"
            )
        }
    }

    @Test
    fun `the ocarina, whose hand shapes don't share a mesh layout, still swaps between them`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(79), attachManagers = false).use {
            val instrument = it.instruments.filterIsInstance<MonophonicInstrument>().single()
            it.stepInstruments(frames = 60, delta = STEP)
            val morphing = it.onEngineThread { morphingHands(instrument) }
            assertTrue(morphing.isEmpty(), "The ocarina's hand shapes can't be morphed between")
            it.throwIfEngineFailed()
        }
    }

    private fun morphingHands(instrument: MonophonicInstrument): List<Geometry> = buildList {
        instrument.clones.first().geometry.depthFirstTraversal { spatial ->
            if (spatial is Geometry && spatial.mesh.hasMorphTargets()) add(spatial)
        }
    }

    private companion object {
        val STEP = 5.milliseconds
    }
}
