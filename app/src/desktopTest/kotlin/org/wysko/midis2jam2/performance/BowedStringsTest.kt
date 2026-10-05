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

import org.wysko.midis2jam2.instrument.family.strings.StringFamilyInstrument
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Boots the bowed strings and drives them through a short part. The bow must stay a valid transform throughout,
 * rest on the strings while notes sound, and be lifted away once the part is over.
 */
class BowedStringsTest {
    private val bowedPrograms = listOf(40 to "violin", 41 to "viola", 42 to "cello", 43 to "contrabass")

    @Test
    @Spec("instrument.strings.bow.lifts")
    fun `the bow is down while notes sound and lifted once the part ends`() {
        for ((program, name) in bowedPrograms) {
            HeadlessPerformance.start(MidiFixtures.singleProgram(program)).use { performance ->
                val bowed = performance.instruments.filterIsInstance<StringFamilyInstrument>().single()

                performance.stepInstruments(frames = 15, delta = 0.1.seconds) // 1.5 s, mid-part
                val playing = performance.onEngineThread { bowed.bowNode.localTranslation.z }
                assertTrue(playing < 1.0f, "The $name bow should rest on the strings mid-part, but it's at z=$playing")

                performance.stepInstruments(frames = 20, delta = 0.1.seconds) // 3.5 s, after the part
                val resting = performance.onEngineThread {
                    with(bowed.bowNode) {
                        assertTrue(
                            localRotation.let { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() && it.w.isFinite() },
                            "The $name bow rotation isn't finite"
                        )
                        localTranslation.z
                    }
                }
                assertTrue(resting > 1.5f, "The $name bow should be lifted after the last note, but it's at z=$resting")
                performance.throwIfEngineFailed()
            }
        }
    }
}
