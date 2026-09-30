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

package org.wysko.midis2jam2.instrument.family.guitar.fretting

/**
 * How lead-like the music is around each slice, and where phrases begin.
 *
 * @property leadness Per slice, from `0` (chordal rhythm playing) to `1` (single-note lead playing).
 * @property phraseStart Per slice, whether it begins a new phrase — after a rest or a held note, where moving the
 * hand up or down the neck costs a player nothing.
 */
class Texture(val leadness: DoubleArray, val phraseStart: BooleanArray)

/**
 * Labels passages as lead or rhythm, so one track that strums a verse and then takes a solo is fingered the right
 * way in each.
 *
 * Around each slice it looks at [WINDOW] seconds either side and measures two things: the share of slices that are
 * single notes, and how many notes are sounding at once on average. A solo is mostly single notes that don't ring
 * over each other; strummed chords and let-ring arpeggios are neither.
 */
object TextureAnalyzer {
    /** How far either side of a slice to look, in seconds. */
    const val WINDOW: Double = 2.0

    /** A gap of at least this many seconds before a slice can make it start a phrase. */
    const val PHRASE_GAP: Double = 0.3

    private const val PHRASE_GAP_TO_TYPICAL = 1.75
    private const val NEIGHBOURS = 8

    /** Analyses [slices] of [notes]. */
    fun analyze(notes: List<FrettingNote>, slices: List<Slice>): Texture {
        if (slices.isEmpty()) return Texture(DoubleArray(0), BooleanArray(0))
        val polyphony = PolyphonyIntegral(notes)
        val times = DoubleArray(slices.size) { slices[it].time }
        val monoPrefix = IntArray(slices.size + 1).also { prefix ->
            slices.forEachIndexed { i, s -> prefix[i + 1] = prefix[i] + if (s.size == 1) 1 else 0 }
        }

        val leadness = DoubleArray(slices.size) { i ->
            val from = lowerBound(times, times[i] - WINDOW)
            val to = upperBound(times, times[i] + WINDOW)
            val count = to - from
            val monoShare = if (count == 0) 1.0 else (monoPrefix[to] - monoPrefix[from]).toDouble() / count
            val sounding = polyphony.average(times[i] - WINDOW, times[i] + WINDOW)
            val fromPolyphony = ((2.0 - sounding) / 0.8).coerceIn(0.0, 1.0)
            val fromShare = ((monoShare - 0.4) / 0.4).coerceIn(0.0, 1.0)
            fromPolyphony * fromShare
        }

        val gaps = DoubleArray(slices.size) { if (it == 0) Double.MAX_VALUE else times[it] - times[it - 1] }
        val phraseStart = BooleanArray(slices.size) { i ->
            if (i == 0) return@BooleanArray true
            val neighbours = ((i - NEIGHBOURS).coerceAtLeast(1)..(i + NEIGHBOURS).coerceAtMost(slices.size - 1))
                .filter { it != i }
                .map { gaps[it] }
                .sorted()
            val typical = if (neighbours.isEmpty()) 0.0 else neighbours[neighbours.size / 2]
            gaps[i] >= PHRASE_GAP && gaps[i] >= typical * PHRASE_GAP_TO_TYPICAL
        }
        return Texture(leadness, phraseStart)
    }

    private fun lowerBound(sorted: DoubleArray, value: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] < value) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun upperBound(sorted: DoubleArray, value: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] <= value) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /**
     * Answers "how many notes sound at once, on average, while anything sounds between two times" in O(log n).
     */
    private class PolyphonyIntegral(notes: List<FrettingNote>) {
        private val times: DoubleArray
        private val level: IntArray
        private val cumulativePolyphony: DoubleArray
        private val cumulativeActive: DoubleArray

        init {
            val events = ArrayList<Pair<Double, Int>>(notes.size * 2)
            notes.forEach {
                events += it.start to 1
                events += maxOf(it.end, it.start) to -1
            }
            events.sortWith(compareBy({ it.first }, { it.second }))
            val t = ArrayList<Double>()
            val l = ArrayList<Int>()
            var current = 0
            var index = 0
            while (index < events.size) {
                val time = events[index].first
                while (index < events.size && events[index].first == time) {
                    current += events[index].second
                    index++
                }
                t += time
                l += current
            }
            times = t.toDoubleArray()
            level = l.toIntArray()
            cumulativePolyphony = DoubleArray(times.size)
            cumulativeActive = DoubleArray(times.size)
            for (k in 1 until times.size) {
                val dt = times[k] - times[k - 1]
                cumulativePolyphony[k] = cumulativePolyphony[k - 1] + level[k - 1] * dt
                cumulativeActive[k] = cumulativeActive[k - 1] + if (level[k - 1] > 0) dt else 0.0
            }
        }

        private fun integrate(t: Double, active: Boolean): Double {
            if (times.isEmpty() || t <= times[0]) return 0.0
            var lo = 0
            var hi = times.size - 1
            while (lo < hi) {
                val mid = (lo + hi + 1) ushr 1
                if (times[mid] <= t) lo = mid else hi = mid - 1
            }
            val base = if (active) cumulativeActive[lo] else cumulativePolyphony[lo]
            val rate = if (active) (if (level[lo] > 0) 1.0 else 0.0) else level[lo].toDouble()
            return base + rate * (t - times[lo])
        }

        fun average(from: Double, to: Double): Double {
            val active = integrate(to, true) - integrate(from, true)
            if (active <= 0.0) return 1.0
            return (integrate(to, false) - integrate(from, false)) / active
        }
    }
}
