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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Tuning for [MalletChoreographer]. Times are in seconds.
 *
 * @property step how finely the mallets' motion is sampled when looking for them passing each other.
 * @property minHold the least time a mallet stays over a bar after striking it, when it has to get out of the way.
 * @property minLead the least time before a strike a mallet arrives over the bar, when it's following another.
 * @property followDelay how long after the mallet in its way leaves that a following mallet sets off.
 * @property relaxation how much the mallets flow through their strikes rather than stopping at each; see [MalletPath].
 */
data class ChoreographyParams(
    val step: Double = 1.0 / 240,
    val minHold: Double = 0.02,
    val minLead: Double = 0.06,
    val followDelay: Double = 0.02,
    val relaxation: Double = MalletPath.DEFAULT_RELAXATION,
)

/**
 * Turns each mallet's strikes into a [MalletPath], timing the moves so the mallets keep out of each other's way.
 *
 * Mallets pass each other whenever one strikes on the far side of another. Often that's avoidable: in a fast
 * descending run played hand to hand, the left mallet can leave its bar the moment it strikes and head for its next
 * note, while the right mallet waits for it to go before following it down. For every place two mallets would pass,
 * this tries those adjustments to when each one departs and arrives, keeping whichever avoids the pass. Where a pass
 * can't be avoided, the faster-moving mallet jumps over the other.
 */
object MalletChoreographer {

    /** Builds the paths for mallets striking [keyframes] (one list per mallet, each in time order). */
    fun choreograph(
        keyframes: List<List<MalletKeyframe>>,
        params: ChoreographyParams = ChoreographyParams(),
    ): List<MalletPath> {
        val timings = keyframes.map { MalletPath.defaultTimings(it).toMutableList() }
        val choreography = Choreography(keyframes, timings, params)

        for (left in keyframes.indices) {
            for (right in left + 1 until keyframes.size) choreography.untangle(left, right)
        }
        for (left in keyframes.indices) {
            for (right in left + 1 until keyframes.size) choreography.markJumps(left, right)
        }

        return keyframes.indices.map { MalletPath(keyframes[it], timings[it], params.relaxation) }
    }

    private class Choreography(
        val keyframes: List<List<MalletKeyframe>>,
        val timings: List<MutableList<SegmentTiming>>,
        val params: ChoreographyParams,
    ) {
        fun x(mallet: Int, time: Double): Double =
            MalletPath.xAt(keyframes[mallet], timings[mallet], time, params.relaxation)

        /** Times at which mallets [a] and [b] pass each other between [from] and [to]. */
        fun passes(a: Int, b: Int, from: Double, to: Double): List<Double> {
            val result = mutableListOf<Double>()
            var lastSign = 0
            var lastTime = from
            var time = from
            while (time <= to) {
                val difference = x(b, time) - x(a, time)
                val sign = if (abs(difference) < EPSILON) 0 else if (difference > 0) 1 else -1
                if (sign != 0) {
                    if (lastSign != 0 && sign != lastSign) result += (lastTime + time) / 2
                    lastSign = sign
                    lastTime = time
                }
                time += params.step
            }
            return result
        }

        fun span(a: Int, b: Int): ClosedFloatingPointRange<Double>? {
            if (keyframes[a].isEmpty() || keyframes[b].isEmpty()) return null
            return min(keyframes[a].first().time, keyframes[b].first().time)..
                max(keyframes[a].last().time, keyframes[b].last().time)
        }

        /** Retimes the moves of mallets [a] and [b] to avoid as many passes between them as possible. */
        fun untangle(a: Int, b: Int) {
            val span = span(a, b) ?: return
            var cursor = span.start
            while (cursor < span.endInclusive) {
                val pass = passes(a, b, cursor, span.endInclusive).firstOrNull() ?: return
                val (mover, blocker) = if (speed(a, pass) >= speed(b, pass)) a to b else b to a
                val moverSegment = MalletPath.segmentAt(keyframes[mover], pass)
                if (moverSegment != null) {
                    // The mallet in the way may be arriving at its bar, or about to leave it: its move out of the way
                    // is often the one that starts after the pass would have happened.
                    val current = MalletPath.segmentAt(keyframes[blocker], pass)
                    val next = (current?.plus(1) ?: keyframes[blocker].indexOfFirst { it.time > pass })
                        .takeIf { it in 0 until keyframes[blocker].lastIndex }
                        ?.takeIf { keyframes[blocker][it].time < keyframes[mover][moverSegment + 1].time }
                    retimeBest(mover, moverSegment, blocker, listOfNotNull(current, next))
                }
                cursor = pass + params.step
            }
        }

        fun speed(mallet: Int, time: Double) = abs(x(mallet, time + params.step) - x(mallet, time - params.step))

        fun window(mallet: Int, segment: Int) =
            keyframes[mallet][segment].time..keyframes[mallet][segment + 1].time

        /**
         * Tries combinations of timing variants for the [mover]'s move and the [blocker]'s moves around it, and keeps
         * the one with the fewest passes. The mover may follow any of the blocker's moves.
         */
        fun retimeBest(mover: Int, moverSegment: Int, blocker: Int, blockerSegments: List<Int>) {
            val windows = blockerSegments.map { window(blocker, it) } + listOf(window(mover, moverSegment))
            val from = windows.minOf { it.start }
            val to = windows.maxOf { it.endInclusive }

            // Every combination of the blocker's variants, one per segment.
            var blockerCombinations = listOf(emptyList<Pair<Int, SegmentTiming>>())
            for (segment in blockerSegments) {
                val options = variants(blocker, segment, null)
                blockerCombinations = blockerCombinations.flatMap { chosen -> options.map { chosen + it } }
            }

            fun apply(blockerChoice: List<Pair<Int, SegmentTiming>>, moverChoice: SegmentTiming) {
                blockerSegments.forEachIndexed { i, segment -> timings[blocker][segment] = blockerChoice[i].second }
                timings[mover][moverSegment] = moverChoice
            }

            var best: Triple<Int, Int, () -> Unit>? = null
            for (blockerChoice in blockerCombinations) {
                val departures = blockerChoice.map { it.second.depart }
                val moverOptions = variants(mover, moverSegment, null) +
                    departures.flatMap { departure ->
                        variants(mover, moverSegment, departure).filter { it.first == FOLLOW_CHANGES }
                    }
                for ((moverChanges, moverChoice) in moverOptions) {
                    apply(blockerChoice, moverChoice)
                    val count = passes(mover, blocker, from, to).size
                    val changes = moverChanges + blockerChoice.sumOf { it.first }
                    // Fewest passes first, then the smallest change from the usual timing.
                    if (best == null || count < best.first || (count == best.first && changes < best.second)) {
                        best = Triple(count, changes) { apply(blockerChoice, moverChoice) }
                    }
                }
            }
            best?.third?.invoke()
        }

        /**
         * The ways a mallet could time [segment], each with how much of a change from the usual timing it is. A
         * following variant sets off just after [otherDeparts].
         */
        fun variants(mallet: Int, segment: Int, otherDeparts: Double?): List<Pair<Int, SegmentTiming>> {
            val start = keyframes[mallet][segment].time
            val end = keyframes[mallet][segment + 1].time
            val gap = end - start
            val current = timings[mallet][segment]
            val early = start + min(params.minHold, gap * SQUEEZE)
            val late = end - min(params.minLead, gap * SQUEEZE)

            return buildList {
                add(0 to current)
                add(1 to current.copy(depart = early))
                add(1 to current.copy(arrive = late))
                add(2 to current.copy(depart = early, arrive = late))
                if (otherDeparts != null) {
                    val follow = max(current.depart, otherDeparts + params.followDelay)
                    if (follow < late - MIN_TRAVEL) add(FOLLOW_CHANGES to current.copy(depart = follow, arrive = late))
                }
            }
        }

        /** Where mallets [a] and [b] still pass each other, the faster-moving one jumps over the other. */
        fun markJumps(a: Int, b: Int) {
            val span = span(a, b) ?: return
            for (pass in passes(a, b, span.start, span.endInclusive)) {
                val (jumper, fallback) = if (speed(a, pass) >= speed(b, pass)) a to b else b to a
                val mallet = if (MalletPath.segmentAt(keyframes[jumper], pass) != null) jumper else fallback
                val segment = MalletPath.segmentAt(keyframes[mallet], pass) ?: continue
                val timing = timings[mallet][segment]
                timings[mallet][segment] = timing.copy(jumps = timing.jumps + pass)
            }
        }
    }

    private const val EPSILON = 1e-6

    /** The squeezed hold or lead takes at most this fraction of the gap between strikes. */
    private const val SQUEEZE = 0.1

    /** The shortest move a following mallet can make. */
    private const val MIN_TRAVEL = 0.02

    private const val FOLLOW_CHANGES = 3
}
