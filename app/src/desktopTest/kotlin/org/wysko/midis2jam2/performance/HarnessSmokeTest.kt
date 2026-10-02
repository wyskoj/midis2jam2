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

import com.jme3.app.state.BaseAppState
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import kotlin.test.Test
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * Tests wait for input to take effect by letting frames pass, so a frame has to be a real
     * engine update. Queued calls alone are not: the engine drains its whole queue each frame,
     * and a test thread that queues quickly enough can fit many "frames" into one update.
     */
    @Test
    fun `letting frames pass lets the engine update that many times`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), attachManagers = false).use { performance ->
            val updates = AtomicInteger()
            performance.onEngineThread {
                performance.app.stateManager.attach(object : BaseAppState() {
                    override fun initialize(app: com.jme3.app.Application) = Unit
                    override fun cleanup(app: com.jme3.app.Application) = Unit
                    override fun onEnable() = Unit
                    override fun onDisable() = Unit
                    override fun update(tpf: Float) {
                        updates.incrementAndGet()
                    }
                })
            }
            InputHarness(performance).frames(1)
            val before = updates.get()

            InputHarness(performance).frames(FRAMES)

            val passed = updates.get() - before
            assertTrue(passed >= FRAMES, "Waiting for $FRAMES frames let only $passed engine updates pass")
        }
    }

    private companion object {
        const val FRAMES = 30
    }
}
