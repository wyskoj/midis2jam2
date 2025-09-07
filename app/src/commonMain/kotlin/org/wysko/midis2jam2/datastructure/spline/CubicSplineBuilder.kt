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

class CubicSplineBuilder {
    fun createSpline(t: List<Double>, x: List<Double>): CubicSpline {
        require(t.size == x.size) { "Arrays must be the same size" }
        require(t.size >= 2) { "At least two points required" }

        val n = t.size - 1
        val h = DoubleArray(n) { i -> t[i + 1] - t[i] }
        val alpha = DoubleArray(n) { i ->
            if (i == 0) 0.0 else 3 * (x[i + 1] - x[i]) / h[i] - 3 * (x[i] - x[i - 1]) / h[i - 1]
        }

        val l = DoubleArray(n + 1)
        val mu = DoubleArray(n)
        val z = DoubleArray(n + 1)
        l[0] = 1.0
        mu[0] = 0.0
        z[0] = 0.0

        for (i in 1 until n) {
            l[i] = 2 * (t[i + 1] - t[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / l[i]
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i]
        }

        l[n] = 1.0
        z[n] = 0.0
        val u = DoubleArray(n + 1)
        u[n] = 0.0

        for (i in n - 1 downTo 0) {
            u[i] = z[i] - mu[i] * u[i + 1]
        }

        val segments = mutableListOf<SplineSegment>()
        for (i in 0 until n) {
            val a = x[i]
            val b = (x[i + 1] - x[i]) / h[i] - h[i] * (u[i + 1] + 2 * u[i]) / 3
            val c = u[i]
            val d = (u[i + 1] - u[i]) / (3 * h[i])
            segments.add(SplineSegment(a, b, c, d, t[i]))
        }

        return CubicSpline(segments, t)
    }
}
