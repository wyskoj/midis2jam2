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
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

/**
 * Steps a performance and checks that the animation maths stays sane.
 *
 * Animation bugs usually show up as a value that has gone to infinity or to not-a-number: an
 * instrument vanishes, or the camera flips inside out. On screen that is obvious and on a
 * build server it is invisible, so the scene graph is checked directly instead.
 */
class TickStabilityTest {

    @Test
    @Spec("app.performance.tick-is-stable")
    fun `stepping the whole band never produces a non-finite transform`() {
        HeadlessPerformance.start(MidiFixtures.theWholeBand()).use { performance ->
            performance.stepInstruments(frames = FRAMES)
            performance.throwIfEngineFailed()

            val bad = performance.onEngineThread { nonFiniteSpatials(performance.performance.root) }
            if (bad.isNotEmpty()) {
                fail(
                    "${bad.size} spatial(s) have a non-finite transform after $FRAMES frames:\n" +
                        bad.take(MAX_REPORTED).joinToString("\n") { "  $it" }
                )
            }
        }
    }

    @Test
    fun `stepping with an unusually long frame does not break the animation`() {
        // A stalled frame - a window drag, a garbage collection - hands the animation a delta
        // far larger than a frame. Nothing should run away as a result.
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            performance.stepInstruments(frames = 10)
            performance.stepInstruments(frames = 1, delta = 5.seconds)
            performance.stepInstruments(frames = 10)
            performance.throwIfEngineFailed()

            val bad = performance.onEngineThread { nonFiniteSpatials(performance.performance.root) }
            assertTrue(bad.isEmpty(), "A long frame produced non-finite transforms: $bad")
        }
    }

    @Test
    fun `stepping a silent file is harmless`() {
        HeadlessPerformance.start(MidiFixtures.empty()).use { performance ->
            performance.stepInstruments(frames = FRAMES)
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `every percussion note can be animated`() {
        HeadlessPerformance.start(MidiFixtures.everyPercussionNote()).use { performance ->
            performance.stepInstruments(frames = FRAMES)
            performance.throwIfEngineFailed()

            val bad = performance.onEngineThread { nonFiniteSpatials(performance.performance.root) }
            assertTrue(bad.isEmpty(), "Percussion produced non-finite transforms: $bad")
        }
    }

    private companion object {

        /** Four seconds of animation at sixty frames a second. */
        const val FRAMES = 240

        const val MAX_REPORTED = 20

        /** Describes every spatial whose transform has stopped being a real number. */
        fun nonFiniteSpatials(root: Spatial): List<String> = buildList {
            fun visit(spatial: Spatial, path: String) {
                val name = spatial.name ?: spatial::class.simpleName.orEmpty()
                val here = if (path.isEmpty()) name else "$path/$name"

                val translation = spatial.localTranslation
                val scale = spatial.localScale
                val rotation = spatial.localRotation

                val problems = buildList {
                    if (!isFinite(translation.x, translation.y, translation.z)) add("translation=$translation")
                    if (!isFinite(scale.x, scale.y, scale.z)) add("scale=$scale")
                    if (!isFinite(rotation.x, rotation.y, rotation.z, rotation.w)) add("rotation=$rotation")
                }
                if (problems.isNotEmpty()) add("$here: ${problems.joinToString(", ")}")

                if (spatial is Node) spatial.children.forEach { visit(it, here) }
            }
            visit(root, "")
        }

        fun isFinite(vararg values: Float): Boolean = values.all { it.isFinite() }
    }
}
