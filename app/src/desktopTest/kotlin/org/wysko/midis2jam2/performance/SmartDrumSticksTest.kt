/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.SnareDrum
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Tom
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.testing.withInstruments
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

/**
 * The smart drum sticks setting: the drum kit played by two sticks that travel around it, instead of every piece
 * having a stick of its own. Boots a real kit and checks that only the two sticks show, that one is on the right
 * piece as each hit sounds, and that the pieces still react to being struck now that their own sticks are hidden.
 */
class SmartDrumSticksTest {

    @Test
    @Spec("instrument.drumset.smart-sticks")
    fun `with smart drum sticks on, two sticks travel to strike every hit`() {
        val sequence = MidiFixtures.drumGroove()

        HeadlessPerformance.start(sequence, SMART).use { performance ->
            val kit = performance.instruments.filterIsInstance<DrumSet>().single()
            assertEquals(2, kit.smartStickNodes.size, "Smart mode should build two sticks")
            assertEquals(
                VISIBLE_IN_SMART_MODE,
                performance.onEngineThread { visibleSticksIn(performance, kit.geometry) },
                "Only the two roaming sticks (and the side stick) should show, not one per piece"
            )

            val hits = sequence.smf.tracks.flatMap { it.events }
                .filterIsInstance<NoteEvent.NoteOn>()
                .filter { it.note.toInt() in TARGET_BY_NOTE }
                .sortedBy { sequence.getTimeOf(it) }

            val clock = Clock(performance)
            for (hit in hits) {
                val time = sequence.getTimeOf(hit).toDouble(SECONDS)
                clock.stepTo(time)

                val target = kit.smartStickTargets.getValue(TARGET_BY_NOTE.getValue(hit.note.toInt()))
                val sticks = performance.onEngineThread { kit.smartStickNodes.map { it.localTranslation.clone() } }
                assertTrue(
                    sticks.any { it.distance(target) < TOLERANCE },
                    "Note ${hit.note} at ${time}s should have a stick on it (at $target), but the sticks are at $sticks"
                )
            }
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `with smart drum sticks on, the drums still recoil when struck`() {
        HeadlessPerformance.start(MidiFixtures.drumGroove(), SMART).use { performance ->
            val kit = performance.instruments.filterIsInstance<DrumSet>().single()
            val snare = kit.pieces.filterIsInstance<SnareDrum>().single()
            val clock = Clock(performance)

            // The first snare hit is on beat two, half a second in.
            clock.stepTo(SNARE_TIME)
            val recoil = performance.onEngineThread { snare.recoilOffset }
            assertTrue(recoil < 0f, "The snare should recoil when struck, but it's at $recoil")

            // The fill starts on the high tom at four seconds.
            clock.stepTo(FILL_TIME)
            val toms = performance.onEngineThread { kit.pieces.filterIsInstance<Tom>().map { it.recoilOffset } }
            assertTrue(toms.any { it < 0f }, "A tom should recoil when the fill starts, but they're at $toms")
        }
    }

    @Test
    fun `with smart drum sticks on, every percussion note can be played`() {
        HeadlessPerformance.start(MidiFixtures.everyPercussionNote(), SMART).use { performance ->
            val kit = performance.instruments.filterIsInstance<DrumSet>().single()
            assertEquals(2, kit.smartStickNodes.size)
            // Big steps: with managers attached the performance ends with the song in real time, so step through
            // the whole file (one note every half second, about thirty seconds) well before that happens.
            performance.stepInstruments(frames = 350, delta = 0.1.seconds)
            performance.throwIfEngineFailed()

            val bad = performance.onEngineThread {
                kit.smartStickNodes.filterNot { node ->
                    with(node.localTranslation) { x.isFinite() && y.isFinite() && z.isFinite() } &&
                        with(node.localRotation) { x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite() }
                }
            }
            assertTrue(bad.isEmpty(), "A stick ended up with a non-finite transform")
        }
    }

    @Test
    fun `with smart drum sticks off, every piece keeps its own stick`() {
        HeadlessPerformance.start(MidiFixtures.drumGroove()).use { performance ->
            val kit = performance.instruments.filterIsInstance<DrumSet>().single()
            assertTrue(kit.smartStickNodes.isEmpty())
            assertEquals(STICKS_PER_KIT, performance.onEngineThread { visibleSticksIn(performance, kit.geometry) })
        }
    }

    /** Steps the performance a frame at a time up to a moment, the way the screen would. */
    private class Clock(private val performance: HeadlessPerformance) {
        private var now = 0.0

        init {
            step(1)
        }

        fun stepTo(time: Double) = step(ceil((time - now) * FPS).toInt())

        private fun step(frames: Int) {
            if (frames <= 0) return
            performance.stepInstruments(frames = frames, delta = (1.0 / FPS).seconds)
            now += frames / FPS
        }
    }

    private companion object {
        const val FPS = 60.0
        /** Pieces are about ten units apart; a frame after a hit, the stick has only just moved on. */
        const val TOLERANCE = 0.5f
        const val SNARE_TIME = 0.5
        const val FILL_TIME = 4.0

        /** The snare, the hi-hat, six toms, four cymbals and two rides, plus the side stick. */
        const val STICKS_PER_KIT = 15

        /** The two roaming sticks and the side stick, which stays put. */
        const val VISIBLE_IN_SMART_MODE = 3

        val SMART = AppSettings().withInstruments { copy(isSmartDrumSticks = true) }

        /** Which smart stick target each hand-played note in [MidiFixtures.drumGroove] is struck at. */
        val TARGET_BY_NOTE = mapOf(
            38 to "snare",
            42 to "hi_hat",
            50 to "tom_high",
            48 to "tom_high_mid",
            47 to "tom_low_mid",
            45 to "tom_low",
            43 to "tom_high_floor",
            41 to "tom_low_floor",
            49 to "cymbal_crash_1",
        )

        /**
         * Counts the stick models under [root] that aren't hidden for good. Loaded models share their meshes with the
         * asset cache, so a stick is any geometry drawing the same mesh as a freshly loaded stick. A ghost stick is
         * hidden on the stick model itself, which is the geometry or its parent.
         */
        fun visibleSticksIn(performance: HeadlessPerformance, root: Spatial): Int {
            val stickMeshes = mutableListOf<Mesh>()
            performance.performance.model(Models.Shared.Stick)
                .depthFirstTraversal { if (it is Geometry) stickMeshes += it.mesh }

            var count = 0
            root.depthFirstTraversal { spatial ->
                if (spatial is Geometry && stickMeshes.any { it === spatial.mesh }) {
                    val ghost = listOfNotNull(spatial, spatial.parent).any {
                        it.localCullHint == Spatial.CullHint.Always
                    }
                    if (!ghost) count++
                }
            }
            return count
        }
    }
}
