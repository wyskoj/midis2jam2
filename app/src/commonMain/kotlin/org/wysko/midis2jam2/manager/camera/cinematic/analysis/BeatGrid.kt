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

package org.wysko.midis2jam2.manager.camera.cinematic.analysis

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.MetaEvent
import kotlin.math.abs
import kotlin.time.DurationUnit.SECONDS

private const val DEFAULT_SECONDS_PER_BEAT = 0.5
private const val DEFAULT_BEATS_PER_BAR = 4

/**
 * The beats and bar lines of a song, in seconds.
 *
 * Beats are the analysis windows of the cinematic camera, and bar lines are where it prefers to cut.
 *
 * @property beats The time of every beat, ascending. There is always at least one.
 * @param barStartBeats The index of every beat that begins a bar.
 */
class BeatGrid(val beats: DoubleArray, barStartBeats: IntArray) {
    init {
        require(beats.isNotEmpty()) { "A beat grid needs at least one beat." }
    }

    private val barStarts = barStartBeats.toSortedSet()

    /** The number of beats in the grid. */
    val beatCount: Int get() = beats.size

    /** The index of every beat that begins a bar, ascending. */
    val barStartBeats: List<Int> = barStarts.toList()

    /** The time of every bar line, ascending. */
    val barTimes: List<Double> = barStartBeats.map { beats[it] }

    /** The beat that [time] falls in: the last beat at or before it, clamped to the grid. */
    fun beatAt(time: Double): Int {
        var low = 0
        var high = beats.lastIndex
        if (time <= beats[0]) return 0
        if (time >= beats[high]) return high
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (beats[mid] <= time) low = mid else high = mid - 1
        }
        return low
    }

    /** The time of [beat], extrapolated at the nearest tempo if it lies outside the grid. */
    fun timeOf(beat: Int): Double = when {
        beat < 0 -> beats[0] + beat * secondsPerBeatAt(0)
        beat > beats.lastIndex -> beats.last() + (beat - beats.lastIndex) * secondsPerBeatAt(beats.lastIndex)
        else -> beats[beat]
    }

    /** The length of [beat] in seconds. */
    fun secondsPerBeatAt(beat: Int): Double = when {
        beats.size < 2 -> DEFAULT_SECONDS_PER_BEAT
        beat >= beats.lastIndex -> beats[beats.lastIndex] - beats[beats.lastIndex - 1]
        else -> beats[beat.coerceAtLeast(0) + 1] - beats[beat.coerceAtLeast(0)]
    }

    /** Whether [beat] begins a bar. */
    fun isBarStart(beat: Int): Boolean = beat in barStarts

    /** The number of beats in the bar that contains [beat]. */
    fun beatsPerBarAt(beat: Int): Int {
        val start = barStarts.headSet(beat + 1).lastOrNull() ?: return DEFAULT_BEATS_PER_BAR
        val next = barStarts.tailSet(start + 1).firstOrNull() ?: return previousBarLength(start)
        return next - start
    }

    private fun previousBarLength(start: Int): Int {
        val previous = barStarts.headSet(start).lastOrNull() ?: return DEFAULT_BEATS_PER_BAR
        return start - previous
    }

    /** The length of the bar that contains [time], in seconds. */
    fun secondsPerBarAt(time: Double): Double {
        val beat = beatAt(time)
        return beatsPerBarAt(beat) * secondsPerBeatAt(beat)
    }

    /** The beat time nearest to [time]. */
    fun nearestBeat(time: Double): Double = nearestOf(beats.asList(), time)

    /** The bar line nearest to [time], or `null` if none is within [tolerance] seconds. */
    fun nearestBarLine(time: Double, tolerance: Double): Double? =
        barTimes.takeIf { it.isNotEmpty() }?.let { nearestOf(it, time) }?.takeIf { abs(it - time) <= tolerance }

    private fun nearestOf(times: List<Double>, time: Double): Double {
        var low = 0
        var high = times.lastIndex
        while (low < high) {
            val mid = (low + high) / 2
            if (times[mid] < time) low = mid + 1 else high = mid
        }
        val after = times[low]
        val before = times.getOrNull(low - 1) ?: return after
        return if (abs(before - time) <= abs(after - time)) before else after
    }

    companion object {
        /**
         * A grid with a constant tempo and meter, covering [duration] seconds.
         */
        fun uniform(
            secondsPerBeat: Double,
            beatsPerBar: Int = DEFAULT_BEATS_PER_BAR,
            duration: Double,
        ): BeatGrid {
            val count = (duration / secondsPerBeat).toInt().coerceAtLeast(0) + 2
            val beats = DoubleArray(count) { it * secondsPerBeat }
            val bars = (0 until count step beatsPerBar).toList().toIntArray()
            return BeatGrid(beats, bars)
        }

        /**
         * Builds the grid for [sequence], following its tempo map and time signatures.
         *
         * The beat is the notated beat of each time signature (the eighth note in 6/8), and the meter defaults to
         * 4/4. Files timed in SMPTE frames have no beats, so they get a steady 120 BPM grid instead.
         */
        fun from(sequence: TimeBasedSequence): BeatGrid {
            val duration = sequence.duration.toDouble(SECONDS)
            val tpq = sequence.smf.tpq.toInt()
            if (tpq <= 0) return uniform(DEFAULT_SECONDS_PER_BEAT, DEFAULT_BEATS_PER_BAR, duration)

            val events = sequence.smf.tracks.flatMap { it.events }
            val lastTick = events.maxOfOrNull { it.tick } ?: 0
            val signatures = events.filterIsInstance<MetaEvent.TimeSignature>()
                .sortedBy { it.tick }
                .distinctBy { it.tick }

            val beats = mutableListOf<Double>()
            val bars = mutableListOf<Int>()
            var signatureIndex = -1
            var numerator = DEFAULT_BEATS_PER_BAR
            var beatTicks = tpq
            var beatInBar = 0
            var tick = 0

            // One beat past the end, so the last notes still have a window to fall in.
            while (tick <= lastTick + beatTicks) {
                while (signatureIndex + 1 < signatures.size && signatures[signatureIndex + 1].tick <= tick) {
                    signatureIndex++
                    val signature = signatures[signatureIndex]
                    numerator = signature.numerator.toInt().coerceAtLeast(1)
                    beatTicks = (tpq * 4 shr signature.denominator.toInt().coerceIn(0, 6)).coerceAtLeast(1)
                    beatInBar = 0
                }
                if (beatInBar == 0) bars += beats.size
                beats += sequence.getTimeAtTick(tick).toDouble(SECONDS)
                beatInBar = (beatInBar + 1) % numerator
                tick += beatTicks
            }

            return BeatGrid(beats.toDoubleArray(), bars.toIntArray())
        }
    }
}
