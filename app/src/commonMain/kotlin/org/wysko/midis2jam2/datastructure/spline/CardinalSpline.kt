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

package org.wysko.midis2jam2.datastructure.spline

class CardinalSpline(
    tRaw: List<Double>,
    xRaw: List<Double>,
    private val tension: Double = 0.25,
) {
    private val t: List<Double>
    private val x: List<Double>
    private val n: Int

    init {
        require(tRaw.size == xRaw.size) { "t and x must be same length" }
        require(tRaw.isNotEmpty()) { "Input cannot be empty" }

        // Filter duplicates: if consecutive t values are equal, keep only the first
        val filtered = tRaw.indices
            .filter { i -> i == 0 || tRaw[i] != tRaw[i - 1] }
            .map { i -> tRaw[i] to xRaw[i] }

        require(filtered.size >= 2) { "Need at least two unique time points" }

        t = filtered.map { it.first }
        x = filtered.map { it.second }
        n = t.size

        require(
            t.zipWithNext().all { it.first < it.second }
        ) { "t must be strictly increasing after filtering duplicates" }
    }

    fun evaluate(tEval: Double): Double {
        if (tEval <= t.first()) return x.first()
        if (tEval >= t.last()) return x.last()

        val i = t.binarySearch(tEval).let { if (it >= 0) it else -it - 2 }

        val t0 = if (i > 0) t[i - 1] else t[i]
        val t1 = t[i]
        val t2 = t[i + 1]
        val t3 = if (i + 2 < n) t[i + 2] else t[i + 1]

        val x0 = if (i > 0) x[i - 1] else x[i]
        val x1 = x[i]
        val x2 = x[i + 1]
        val x3 = if (i + 2 < n) x[i + 2] else x[i + 1]

        val dt = (tEval - t1) / (t2 - t1)

        val m1 = (1 - tension) * (x2 - x0) / (t2 - t0)
        val m2 = (1 - tension) * (x3 - x1) / (t3 - t1)

        val h00 = (2 * dt * dt * dt) - (3 * dt * dt) + 1
        val h10 = (dt * dt * dt) - (2 * dt * dt) + dt
        val h01 = (-2 * dt * dt * dt) + (3 * dt * dt)
        val h11 = (dt * dt * dt) - (dt * dt)

        return h00 * x1 + h10 * (t2 - t1) * m1 + h01 * x2 + h11 * (t2 - t1) * m2
    }
}
