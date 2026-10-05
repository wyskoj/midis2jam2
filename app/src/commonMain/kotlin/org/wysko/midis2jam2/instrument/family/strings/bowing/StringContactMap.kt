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

package org.wysko.midis2jam2.instrument.family.strings.bowing

/**
 * The straight line the bow hair lies along when seen end-on: height [z0] above the instrument at the bridge's
 * centre (`x = 0`), rising by [slope] for each unit of `x`.
 */
data class ContactLine(val z0: Double, val slope: Double)

/**
 * Where the strings cross the bridge, and so how the bow must be tilted to sound a string or a pair of them.
 *
 * The bridge is an arch, so the bow can't lie flat across all four strings: a player tilts it about the strings'
 * direction until the hair rests on the strings they want and clears the rest. Because the arch is convex, a line
 * through one string at the arch's local slope, or through two adjacent strings, clears every other string.
 *
 * @param x The sideways position of each string at the bridge, lowest string first.
 * @param z The height of each string at the bridge.
 */
class StringContactMap(private val x: DoubleArray, private val z: DoubleArray) {
    init {
        require(x.size == z.size && x.size >= 2) { "Need matching positions for at least two strings" }
    }

    /** The number of strings. */
    val stringCount: Int get() = x.size

    /** The line the bow hair should lie along to sound [strings], which must not be empty. */
    fun lineFor(strings: Set<Int>): ContactLine {
        val sorted = strings.map { it.coerceIn(0, stringCount - 1) }.distinct().sorted()
        val (a, b) = sorted.first() to sorted.last()
        val slope = if (a != b) chord(a, b) else tangent(a)
        // Anchor on the lowest sounded string; for a pair the line passes through both.
        return ContactLine(z0 = z[a] - slope * x[a], slope = slope)
    }

    private fun chord(a: Int, b: Int) = (z[b] - z[a]) / (x[b] - x[a])

    private fun tangent(i: Int): Double = when (i) {
        0 -> chord(0, 1)
        stringCount - 1 -> chord(i - 1, i)
        else -> (chord(i - 1, i) + chord(i, i + 1)) / 2
    }
}
