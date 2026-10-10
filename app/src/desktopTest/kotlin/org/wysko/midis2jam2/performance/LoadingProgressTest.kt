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

import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.manager.LoadingProgressManager
import org.wysko.midis2jam2.starter.LoadingStage
import org.wysko.midis2jam2.starter.ProgressListener
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The loading progress that Android's loading screen draws.
 *
 * Band-building progress used to be the channel number over 16, which jumped over empty channels and never counted
 * the drums. And the listener used to be registered by spinning until the manager was attached, which a manager that
 * exists from the start no longer needs, as long as a listener that registers late is caught up.
 */
class LoadingProgressTest {

    @Test
    fun `building the band reports progress that only rises and ends complete`() {
        val file = MidiFixtures.theWholeBand()
        HeadlessPerformance.start(file).use { performance ->
            val reported = mutableListOf<Float>()
            performance.onEngineThread {
                InstrumentAssignment.assign(performance.performance, file, reported::add)
            }

            assertTrue(reported.size > 1, "Building a whole band should report progress more than once: $reported")
            assertTrue(reported.all { it in 0f..1f }, "Progress should stay between 0 and 1: $reported")
            assertEquals(reported.sorted(), reported, "Progress should never go backwards")
            assertEquals(1f, reported.last(), "Progress should end complete")
        }
    }

    @Test
    fun `a listener that registers part-way through loading is told where loading has got to`() {
        val manager = LoadingProgressManager()
        manager.onLoadingStage(LoadingStage.BuildingBand)
        manager.onLoadingProgress(0.5f)

        val heard = mutableListOf<String>()
        manager.registerProgressListener(object : ProgressListener {
            override fun onLoadingStage(stage: LoadingStage) {
                heard += "stage $stage"
            }

            override fun onLoadingProgress(progress: Float) {
                heard += "progress $progress"
            }

            override fun onReady() {
                heard += "ready"
            }
        })
        manager.onLoadingProgress(0.75f)

        assertEquals(listOf("stage BuildingBand", "progress 0.5", "progress 0.75"), heard)
    }
}
