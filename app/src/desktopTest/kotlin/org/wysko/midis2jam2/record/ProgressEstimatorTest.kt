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

package org.wysko.midis2jam2.record

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * The "about 1:10 left" shown while recording.
 *
 * A render starts slowly (loading, compiling shaders) and then runs at a very even pace. The estimate must reflect
 * that even pace, not be dragged out by the slow start, and it must stay quiet until it has something to go on.
 */
class ProgressEstimatorTest {
    private var clock = ZERO
    private val estimator = ProgressEstimator { clock.inWholeNanoseconds }

    @Test
    fun `it stays quiet until it has seen enough`() {
        assertNull(report(0.seconds, 0f), "The first report comes before the render warms up, and is ignored")
        assertNull(report(1.seconds, 0.01f), "Only one usable report")
        assertNull(report(2.seconds, 0.02f), "Under three seconds of reports")
    }

    @Test
    fun `a steady pace gives the time the rest will take`() {
        steady(from = 0, to = 10, percentPerSecond = 2)

        val remaining = assertNotNull(report(11.seconds, 0.22f))

        assertAbout(39.seconds, remaining) // 78% left at 2% a second
    }

    @Test
    fun `a slow start is forgotten once the render settles`() {
        // Ten seconds to get going, reaching only 1%; then a steady 2% a second.
        report(0.seconds, 0f)
        report(10.seconds, 0.01f)
        var progress = 0.01f
        for (second in 11..30) {
            progress += 0.02f
            report(second.seconds, progress)
        }

        val remaining = assertNotNull(report(31.seconds, progress + 0.02f))

        assertAbout(((1f - progress - 0.02f) / 0.02f).toDouble().seconds, remaining)
    }

    @Test
    fun `speeding up brings the estimate down`() {
        steady(from = 0, to = 10, percentPerSecond = 1)
        val before = assertNotNull(report(11.seconds, 0.11f))
        var progress = 0.11f
        for (second in 12..16) {
            progress += 0.04f
            report(second.seconds, progress)
        }
        val after = assertNotNull(report(17.seconds, progress + 0.04f))

        assertTrue(after < before, "Faster progress should shorten the estimate, but went $before -> $after")
    }

    @Test
    fun `nothing is left once the recording is done`() {
        report(0.seconds, 0f)

        assertEquals(ZERO, report(3.seconds, 1f))
    }

    @Test
    fun `a reset starts the estimate over`() {
        steady(from = 0, to = 10, percentPerSecond = 2)

        estimator.reset()

        assertNull(report(11.seconds, 0.22f), "A new recording starts with no estimate")
    }

    private fun report(at: Duration, progress: Float): Duration? {
        clock = at
        return estimator.remaining(progress)
    }

    private fun steady(from: Int, to: Int, percentPerSecond: Int) {
        for (second in from..to) report(second.seconds, second * percentPerSecond / 100f)
    }

    private fun assertAbout(expected: Duration, actual: Duration) =
        assertTrue((expected - actual).absoluteValue < 1.seconds, "Expected about $expected, but was $actual")
}
