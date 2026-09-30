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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A point in the instrument's local space. */
data class Point3(val x: Double, val y: Double, val z: Double) {
    internal fun lerp(to: Point3, t: Double): Point3 = when {
        t <= 0.0 -> this
        t >= 1.0 -> to
        else -> Point3(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t)
    }
}

/**
 * Where a mallet has to be, and when.
 *
 * @property time when the mallet strikes, in seconds.
 * @property point where the mallet head strikes.
 */
data class MalletKeyframe(val time: Double, val point: Point3)

/**
 * When a mallet makes the move between two of its strikes.
 *
 * @property depart when it leaves the bar it last struck.
 * @property arrive when it's over the next bar.
 * @property jumps when, during the move, it passes over another mallet and has to lift over it.
 */
data class SegmentTiming(val depart: Double, val arrive: Double, val jumps: List<Double> = emptyList())

/**
 * Where a roaming mallet is over time, given the strikes it has been assigned.
 *
 * Two motions are blended, by [relaxation]:
 * - A *stiff* one: the mallet sits over the bar it last struck while it recoils, glides to the next bar with an
 *   eased move, and arrives before its wind-up starts so it comes down on the right bar. When strikes are close
 *   together the move squeezes into the gap between them. [MalletChoreographer] adjusts these [timings] so mallets
 *   keep out of each other's way.
 * - A *flowing* one: a smooth curve through the strikes (a monotone cubic spline), so in a run the mallet keeps moving
 *   through each bar instead of stopping at it, and only slows to a stop where it turns around. Being monotone, it
 *   never overshoots a bar.
 *
 * Either way the mallet is exactly over each bar as it strikes it. It lifts slightly while traveling so it doesn't
 * skim through the bars, and jumps higher at the moments it passes over another mallet.
 *
 * @param keyframes the strikes, in time order.
 * @param timings for each gap between strikes, when the stiff motion moves. Defaults to [defaultTiming].
 * @param relaxation how much of the flowing motion to use, from 0 (all stiff) to 1 (all flowing).
 * @param lift how high the mallet rises while traveling, per unit of distance traveled.
 * @param maxLift the most the mallet rises while traveling.
 * @param jumpHeight how high the mallet rises to pass over another mallet.
 */
class MalletPath(
    val keyframes: List<MalletKeyframe>,
    val timings: List<SegmentTiming> = defaultTimings(keyframes),
    private val relaxation: Double = DEFAULT_RELAXATION,
    private val lift: Double = 0.08,
    private val maxLift: Double = 2.0,
    private val jumpHeight: Double = 3.0,
) {
    init {
        require(keyframes.zipWithNext().all { (a, b) -> a.time <= b.time }) { "Keyframes must be in time order" }
        require(timings.size == max(0, keyframes.size - 1)) { "Need one timing per gap between keyframes" }
        require(relaxation in 0.0..1.0) { "Relaxation must be between 0 and 1, got $relaxation" }
    }

    /** Where the mallet is at [time] (seconds). With [lifted] false, the lift and jumps are left out (for shadows). */
    fun positionAt(time: Double, lifted: Boolean = true): Point3 {
        val i = segmentAt(keyframes, time) ?: return holdAt(time)
        val a = keyframes[i]
        val b = keyframes[i + 1]
        val timing = timings[i]
        val point = pointAt(keyframes, timings, i, time, relaxation)
        if (!lifted) return point

        // Arc over the ground actually covered, so the lift follows the mallet however it's moving.
        val travel = b.point.x - a.point.x
        val covered = if (abs(travel) > 1e-9) ((point.x - a.point.x) / travel).coerceIn(0.0, 1.0) else 0.0
        val arc = min(abs(travel) * lift, maxLift) * sin(PI * covered)
        val jump = jumpHeight * jumpEnvelope(a.time, b.time, timing, time)
        return point.copy(y = point.y + max(arc, jump))
    }

    private fun holdAt(time: Double): Point3 = when {
        keyframes.isEmpty() -> Point3(0.0, 0.0, 0.0)
        time <= keyframes.first().time -> keyframes.first().point
        else -> keyframes.last().point
    }

    /** How lifted the mallet is (0 to 1) for its [SegmentTiming.jumps], always back down by the strikes at the ends. */
    private fun jumpEnvelope(start: Double, end: Double, timing: SegmentTiming, time: Double): Double {
        if (timing.jumps.isEmpty() || end <= start) return 0.0
        val halfWidth = ((timing.arrive - timing.depart) / 2).coerceIn(MIN_JUMP_HALF_WIDTH, MAX_JUMP_HALF_WIDTH)
        val bump = timing.jumps.maxOf { jump -> 1 - ease(abs(time - jump) / halfWidth) }
        val clearOfStrikes = min(1.0, 4 * sin(PI * ((time - start) / (end - start)).coerceIn(0.0, 1.0)))
        return bump * clearOfStrikes
    }

    companion object {
        /** How much of the flowing motion mallets use by default. */
        const val DEFAULT_RELAXATION = 0.7

        /** How long the mallet stays over a bar after striking it, if there's time. */
        const val RECOIL_HOLD = 0.1

        /** How long before a strike the mallet is over the bar, if there's time. Matches the striker's wind-up. */
        const val ARRIVE_LEAD = 0.22

        /** The hold at each end of a gap takes at most this fraction of it, leaving the rest for the move. */
        private const val TRAVEL_MARGIN = 0.3

        private const val MIN_JUMP_HALF_WIDTH = 0.04
        private const val MAX_JUMP_HALF_WIDTH = 0.15

        /** The usual timing of the move from [a] to [b]. */
        fun defaultTiming(a: MalletKeyframe, b: MalletKeyframe): SegmentTiming {
            val gap = b.time - a.time
            return SegmentTiming(a.time + min(RECOIL_HOLD, gap * TRAVEL_MARGIN), b.time - min(ARRIVE_LEAD, gap * TRAVEL_MARGIN))
        }

        /** The usual timing of every move between [keyframes]. */
        fun defaultTimings(keyframes: List<MalletKeyframe>): List<SegmentTiming> =
            keyframes.zipWithNext { a, b -> defaultTiming(a, b) }

        /**
         * The index of the gap between [keyframes] that [time] falls in, or `null` if it's before the first or at or
         * after the last.
         */
        internal fun segmentAt(keyframes: List<MalletKeyframe>, time: Double): Int? {
            if (keyframes.size < 2 || time <= keyframes.first().time || time >= keyframes.last().time) return null
            var low = 0
            var high = keyframes.lastIndex
            while (high - low > 1) {
                val mid = (low + high) / 2
                if (keyframes[mid].time <= time) low = mid else high = mid
            }
            return low
        }

        /** How far through its move (0 to 1) a mallet with [timing] is at [time]. */
        internal fun progress(timing: SegmentTiming, time: Double): Double = when {
            time <= timing.depart -> 0.0
            time >= timing.arrive -> 1.0
            else -> (time - timing.depart) / (timing.arrive - timing.depart)
        }

        internal fun ease(u: Double): Double {
            val t = u.coerceIn(0.0, 1.0)
            return t * t * (3 - 2 * t)
        }

        /** The horizontal position at [time] of a mallet striking [keyframes] with [timings] and [relaxation]. */
        internal fun xAt(
            keyframes: List<MalletKeyframe>,
            timings: List<SegmentTiming>,
            time: Double,
            relaxation: Double,
        ): Double {
            val i = segmentAt(keyframes, time) ?: return when {
                keyframes.isEmpty() -> 0.0
                time <= keyframes.first().time -> keyframes.first().point.x
                else -> keyframes.last().point.x
            }
            return pointAt(keyframes, timings, i, time, relaxation).x
        }

        /** Where the mallet is (unlifted) at [time], which falls in the gap after keyframe [i]. */
        internal fun pointAt(
            keyframes: List<MalletKeyframe>,
            timings: List<SegmentTiming>,
            i: Int,
            time: Double,
            relaxation: Double,
        ): Point3 {
            val stiff = keyframes[i].point.lerp(keyframes[i + 1].point, ease(progress(timings[i], time)))
            if (relaxation <= 0.0) return stiff
            return stiff.lerp(flowing(keyframes, i, time), relaxation)
        }

        /**
         * The flowing motion at [time], in the gap after keyframe [i]: a cubic Hermite curve through the strikes, with
         * the tangent at each strike chosen so the curve never overshoots (Fritsch and Butland's monotone rule).
         */
        private fun flowing(keyframes: List<MalletKeyframe>, i: Int, time: Double): Point3 {
            val a = keyframes[i]
            val b = keyframes[i + 1]
            val h = b.time - a.time
            if (h <= 0.0) return b.point
            val s = ((time - a.time) / h).coerceIn(0.0, 1.0)

            val s2 = s * s
            val s3 = s2 * s
            val h00 = 2 * s3 - 3 * s2 + 1
            val h10 = s3 - 2 * s2 + s
            val h01 = -2 * s3 + 3 * s2
            val h11 = s3 - s2

            fun component(of: (Point3) -> Double): Double =
                h00 * of(a.point) + h10 * h * tangent(keyframes, i, of) +
                    h01 * of(b.point) + h11 * h * tangent(keyframes, i + 1, of)

            return Point3(component { it.x }, component { it.y }, component { it.z })
        }

        /**
         * The rate of change of one component of the flowing curve as it passes keyframe [k]. Zero at the first and
         * last strikes and wherever the mallet turns around, so it comes to rest there rather than overshooting.
         */
        private fun tangent(keyframes: List<MalletKeyframe>, k: Int, of: (Point3) -> Double): Double {
            if (k <= 0 || k >= keyframes.lastIndex) return 0.0
            val before = keyframes[k].time - keyframes[k - 1].time
            val after = keyframes[k + 1].time - keyframes[k].time
            if (before <= 0.0 || after <= 0.0) return 0.0

            val slopeBefore = (of(keyframes[k].point) - of(keyframes[k - 1].point)) / before
            val slopeAfter = (of(keyframes[k + 1].point) - of(keyframes[k].point)) / after
            if (slopeBefore * slopeAfter <= 0.0) return 0.0

            val weightBefore = 2 * after + before
            val weightAfter = after + 2 * before
            return (weightBefore + weightAfter) / (weightBefore / slopeBefore + weightAfter / slopeAfter)
        }
    }
}
