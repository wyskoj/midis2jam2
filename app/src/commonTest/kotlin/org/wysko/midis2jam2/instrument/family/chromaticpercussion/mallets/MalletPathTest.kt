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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The motion of a roaming mallet between its strikes. What matters on screen: the mallet is exactly over the bar
 * when it strikes, travels directly between bars without overshooting, and (when relaxed) flows through a run rather
 * than stopping dead at every bar.
 */
class MalletPathTest {

    private val a = MalletKeyframe(1.0, Point3(-10.0, 1.35, 12.0))
    private val b = MalletKeyframe(3.0, Point3(10.0, 2.6, 4.0))
    private val path = MalletPath(listOf(a, b))
    private val stiff = MalletPath(listOf(a, b), relaxation = 0.0)

    @Test
    fun `the mallet is over the bar at the moment it strikes`() {
        for (relaxation in listOf(0.0, 0.5, 1.0)) {
            val path = MalletPath(run(0.0, 5.0, 10.0, 5.0), relaxation = relaxation)
            path.keyframes.forEach { assertEquals(it.point, path.positionAt(it.time), "relaxation $relaxation") }
        }
    }

    @Test
    fun `the mallet waits at its first bar before and its last bar after`() {
        assertEquals(a.point, path.positionAt(0.0))
        assertEquals(b.point, path.positionAt(10.0))
    }

    @Test
    fun `a stiff mallet arrives before its wind-up starts`() {
        assertEquals(b.point, stiff.positionAt(b.time - 0.22, lifted = false))
    }

    @Test
    fun `a stiff mallet stays over the bar it struck while it recoils`() {
        assertEquals(a.point, stiff.positionAt(a.time + 0.05))
    }

    @Test
    fun `a relaxed mallet keeps moving through a bar in the middle of a run`() {
        val keyframes = run(0.0, 5.0, 10.0)
        val speedThroughMiddle = { relaxation: Double ->
            val path = MalletPath(keyframes, relaxation = relaxation)
            (path.positionAt(1.01, lifted = false).x - path.positionAt(0.99, lifted = false).x) / 0.02
        }
        assertEquals(0.0, speedThroughMiddle(0.0), 1e-9, "A stiff mallet stops at each bar")
        assertTrue(speedThroughMiddle(1.0) > 3.0, "A relaxed mallet should flow through, got ${speedThroughMiddle(1.0)}")
    }

    @Test
    fun `a relaxed mallet stops where it turns around, without overshooting`() {
        val path = MalletPath(run(0.0, 5.0, 0.0), relaxation = 1.0)
        val speed = (path.positionAt(1.01, lifted = false).x - path.positionAt(0.99, lifted = false).x) / 0.02
        assertEquals(0.0, speed, 0.1)

        var time = 0.0
        while (time <= 2.0) {
            val x = path.positionAt(time).x
            assertTrue(x in 0.0..5.0, "At ${time}s the mallet overshot to x=$x")
            time += 0.01
        }
    }

    @Test
    fun `the mallet travels directly between bars`() {
        var previous = a.point.x
        for (step in 0..200) {
            val x = path.positionAt(a.time + step * 0.01).x
            assertTrue(x in a.point.x..b.point.x, "x=$x left the span between the two bars")
            assertTrue(x >= previous, "The mallet went backwards at step $step")
            previous = x
        }
    }

    @Test
    fun `the mallet lifts while it travels but its shadow does not`() {
        val mid = (a.time + b.time) / 2
        val lifted = path.positionAt(mid)
        val flat = path.positionAt(mid, lifted = false)
        assertTrue(lifted.y > flat.y, "Expected a lift mid-travel, got $lifted vs $flat")
        assertEquals(flat.x, lifted.x)
    }

    @Test
    fun `strikes close together still move the mallet between them`() {
        val quick = MalletPath(listOf(MalletKeyframe(0.0, Point3(0.0, 0.0, 0.0)), MalletKeyframe(0.1, Point3(5.0, 0.0, 0.0))))
        assertEquals(0.0, quick.positionAt(0.0).x)
        assertEquals(5.0, quick.positionAt(0.1).x)
        val x = quick.positionAt(0.05).x
        assertTrue(x > 0.0 && x < 5.0, "Mid-gap the mallet should be traveling, got x=$x")
    }

    @Test
    fun `a mallet with no strikes sits still`() {
        assertEquals(Point3(0.0, 0.0, 0.0), MalletPath(emptyList()).positionAt(1.0))
    }

    private companion object {
        /** Strikes one second apart at each of [xs]. */
        fun run(vararg xs: Double) = xs.mapIndexed { i, x -> MalletKeyframe(i.toDouble(), Point3(x, 1.35, 0.0)) }
    }
}
