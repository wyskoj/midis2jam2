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

import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.testing.withInstruments
import org.wysko.midis2jam2.world.modelD
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

/**
 * The smart mallets setting: a mallet instrument played by two mallets that travel between the bars, instead of one
 * mallet per bar. Boots a real marimba and checks that the mallets exist and are over the right bar as each note
 * sounds, since a mallet striking thin air is exactly what the feature exists to avoid.
 */
class SmartMalletsTest {

    @Test
    @Spec("instrument.mallets.smart")
    fun `with smart mallets on, two mallets travel to strike every note`() {
        val sequence = MidiFixtures.malletRun()
        val smart = AppSettings().withInstruments { copy(isSmartMallets = true) }

        HeadlessPerformance.start(sequence, smart, attachManagers = false).use { performance ->
            val mallets = performance.instruments.filterIsInstance<Mallets>().single()
            assertEquals(2, mallets.roamingMalletNodes.size, "Smart mode should build two mallets")
            assertEquals(
                2,
                performance.onEngineThread { malletModelsIn(performance, mallets.root) },
                "Only the two roaming mallets should be in the scene, not one per bar"
            )

            performance.stepInstruments(frames = 1, delta = (1.0 / FPS).seconds)
            var now = 1 / FPS
            val notes = sequence.smf.tracks.flatMap { it.events }
                .filterIsInstance<NoteEvent.NoteOn>()
                .sortedBy { sequence.getTimeOf(it) }
            for (note in notes) {
                val time = sequence.getTimeOf(note).toDouble(SECONDS)
                val frames = ceil((time - now) * FPS).toInt()
                if (frames > 0) {
                    performance.stepInstruments(frames = frames, delta = (1.0 / FPS).seconds)
                    now += frames / FPS
                }

                val target = mallets.strikePointOf(note.note.toInt())
                val xs = performance.onEngineThread { mallets.roamingMalletNodes.map { it.localTranslation.x } }
                assertTrue(
                    xs.any { abs(it - target.x) < TOLERANCE },
                    "Note ${note.note} at ${time}s should have a mallet over its bar (x=${target.x}), " +
                        "but the mallets are at $xs"
                )
            }
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `with smart mallets off, every bar keeps its own mallet`() {
        HeadlessPerformance.start(MidiFixtures.malletRun(), attachManagers = false).use { performance ->
            val mallets = performance.instruments.filterIsInstance<Mallets>().single()
            assertTrue(mallets.roamingMalletNodes.isEmpty())
            assertEquals(BARS, performance.onEngineThread { malletModelsIn(performance, mallets.root) })
        }
    }

    private companion object {
        const val FPS = 60.0

        /** Mallets strike a little to either side of the bar's center, so two can share a bar. */
        const val TOLERANCE = 0.3f

        /** The mallet instruments have a bar for every note from A0 to C8. */
        const val BARS = 88

        /**
         * Counts the mallet models under [root]. Loaded models share their meshes with the asset cache, so a mallet is
         * any geometry drawing the same mesh as a freshly loaded mallet.
         */
        fun malletModelsIn(performance: HeadlessPerformance, root: Spatial): Int {
            val malletMeshes = mutableListOf<Mesh>()
            performance.performance.modelD("XylophoneMalletWhite.obj", "XylophoneBar.bmp")
                .depthFirstTraversal { if (it is Geometry) malletMeshes += it.mesh }

            var count = 0
            root.depthFirstTraversal { spatial ->
                if (spatial is Geometry && malletMeshes.any { it === spatial.mesh }) count++
            }
            return count
        }
    }
}
