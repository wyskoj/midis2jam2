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

import kotlin.math.pow

class CubicSpline(private val segments: List<SplineSegment>, private val tValues: List<Double>) {
    fun evaluate(t: Double): Double = when {
        t > segments.last().tStart -> segments.last().a
        t < segments.first().tStart -> segments.first().a
        else -> {
            val index = findSegmentIndex(t)
            val segment = segments[index]
            val dt = t - segment.tStart
            segment.a + segment.b * dt + segment.c * dt.pow(2) + segment.d * dt.pow(3)
        }
    }

    private fun findSegmentIndex(t: Double): Int {
        if (t <= tValues.first()) return 0
        if (t >= tValues.last()) return segments.size - 1
        return tValues.binarySearch(t).let {
            if (it >= 0) it.coerceAtMost(segments.size - 1) else -it - 2
        }
    }
}
