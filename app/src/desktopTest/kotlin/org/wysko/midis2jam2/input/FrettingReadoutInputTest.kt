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

package org.wysko.midis2jam2.input

import com.jme3.input.KeyInput
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The live fretting readout, toggled with F4 through the real key bindings.
 *
 * It exists so the fretting engine's decisions can be watched while a song plays: what it inferred for the whole
 * part, and what it is doing at each moment.
 */
class FrettingReadoutInputTest {

    @Test
    @Spec("app.debug.fretting-readout")
    fun `F4 shows the fretting readout above the guitar, keeps it current, and hides it again`() {
        HeadlessPerformance.start(MidiFixtures.dropDRiff()).use { performance ->
            val input = InputHarness(performance)
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()

            performance.stepInstruments(10)
            assertFalse(performance.onEngineThread { guitar.readout.isShowing }, "The readout should start hidden")

            input.tap(KeyInput.KEY_F4)
            performance.stepInstruments(1)
            val (showing, first) = performance.onEngineThread { guitar.readout.isShowing to guitar.readout.text.text }
            assertTrue(showing, "F4 should show the readout")
            assertTrue("Drop D" in first, "The readout should name the inferred tuning:\n$first")

            performance.stepInstruments(60)
            val later = performance.onEngineThread { guitar.readout.text.text }
            assertNotEquals(clockLine(first), clockLine(later), "The readout should follow the song")

            input.tap(KeyInput.KEY_F4)
            performance.stepInstruments(1)
            assertFalse(performance.onEngineThread { guitar.readout.isShowing }, "A second F4 should hide the readout")
        }
    }

    private fun clockLine(text: String) = text.lines().first { it.startsWith("t ") }
}
