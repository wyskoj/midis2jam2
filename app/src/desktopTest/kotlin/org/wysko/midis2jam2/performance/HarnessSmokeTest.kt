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

import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import kotlin.test.Test
import kotlin.test.assertTrue

class HarnessSmokeTest {

    @Test
    fun `a single piano part builds one instrument`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            println("instruments = " + performance.instruments.map { it::class.simpleName })
            assertTrue(performance.instruments.isNotEmpty(), "No instruments were built")
        }
    }

    @Test
    fun `instruments can be ticked`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 40)).use { performance ->
            performance.stepInstruments(frames = 30)
            assertTrue(performance.instruments.isNotEmpty())
        }
    }

    @Test
    fun `the shipped managers attach`() {
        HeadlessPerformance.start(
            MidiFixtures.singleProgram(program = 0),
            attachManagers = true,
        ).use { performance ->
            println("managers = " + performance.managers.map { it::class.simpleName })
            assertTrue(performance.managers.isNotEmpty(), "No managers were attached")
        }
    }
}
