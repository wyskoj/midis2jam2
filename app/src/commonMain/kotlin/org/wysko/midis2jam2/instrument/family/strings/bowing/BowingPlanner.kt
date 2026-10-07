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

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** A note on a bowed string, in seconds. [string] is `0` for the lowest string. */
data class BowNote(val start: Double, val end: Double, val string: Int, val velocity: Int = 80)

/** Which way the bow travels. A down-bow runs from the frog (position `0`) to the tip (position `1`). */
enum class BowDirection { Down, Up }

/**
 * One continuous bow stroke, possibly slurring several notes.
 *
 * @property startPos Where the stroke starts along the bow: `0` at the frog, `1` at the tip.
 * @property endPos Where it ends.
 * @property retake Whether the bow is lifted and returned to the start before this stroke.
 * @property events The notes sounded during the stroke, in order. Simultaneous notes (a double stop) share an event.
 */
data class BowStroke(
    val start: Double,
    val end: Double,
    val direction: BowDirection,
    val startPos: Double,
    val endPos: Double,
    val retake: Boolean,
    val events: List<BowEvent>,
) {
    /** The strings sounding at [time], or those of the nearest event if between events. */
    fun stringsAt(time: Double): Set<Int> =
        (events.lastOrNull { it.start <= time } ?: events.first()).strings

    /** Where the bow is along its length at [time], eased in and out. */
    fun positionAt(time: Double): Double {
        val t = ((time - start) / max(end - start, 1e-6)).coerceIn(0.0, 1.0)
        val eased = t * t * (3 - 2 * t)
        return startPos + (endPos - startPos) * eased
    }
}

/** Notes that sound together: a single note or a double stop. */
data class BowEvent(val start: Double, val end: Double, val strings: Set<Int>, val velocity: Int)

/**
 * Tunable behaviour of the [BowingPlanner].
 *
 * @property fullBowTime How long a note must last, in seconds, before it uses most of the bow. Shorter notes use
 * proportionally less, but even the shortest still sweeps [minTravel] of the bow.
 * @property minTravel The shortest stroke, as a fraction of the bow.
 * @property maxSlur The longest a single stroke may last, in seconds.
 * @property slurGap The largest silence, in seconds, across which notes may still be slurred.
 * @property shortNote Notes shorter than this, in seconds, are articulated with a stroke each.
 * @property retakeGap The shortest silence, in seconds, in which the bow can be lifted and reset.
 */
data class BowingProfile(
    val fullBowTime: Double = 0.7,
    val minTravel: Double = 0.2,
    val maxSlur: Double = 4.0,
    val slurGap: Double = 0.08,
    val shortNote: Double = 0.12,
    val retakeGap: Double = 0.15,
) {
    companion object {
        /** Violin, viola and fiddle. */
        val Small = BowingProfile()

        /** Cello. */
        val Cello = BowingProfile(fullBowTime = 0.9)

        /** Double bass, whose heavy bow moves more slowly. */
        val Bass = BowingProfile(fullBowTime = 1.2)
    }
}

/** The result of [BowingPlanner.plan]. */
class BowingPlan(val strokes: List<BowStroke>) {
    /** The stroke sounding at [time], or `null` between strokes. */
    fun strokeAt(time: Double): BowStroke? = strokes.lastOrNull { it.start <= time && time <= it.end }

    /** The first stroke that begins after [time]. */
    fun nextAfter(time: Double): BowStroke? = strokes.firstOrNull { it.start > time }
}

/**
 * Decides how a player would bow a part: which notes share a slur, which way each stroke goes, and where on the
 * bow it happens.
 *
 * Directions are chosen with a small beam search so that the bow doesn't run out, strong beats favour down-bows,
 * and the bow is only reset (a retake) when there's time to do it.
 */
object BowingPlanner {
    private const val BEAM_WIDTH = 8
    private const val DOUBLE_STOP_WINDOW = 0.03
    private const val RETAKE_COST = 1.0
    private const val NO_TIME_FOR_RETAKE_COST = 6.0
    private const val WRONG_BEAT_COST = 0.6
    private const val RUNNING_OUT_COST = 3.0

    /**
     * Plans [notes] for [profile]. [strongBeat] says whether a time (in seconds) falls on a strong beat, which
     * pulls the stroke toward a down-bow.
     */
    fun plan(
        notes: List<BowNote>,
        profile: BowingProfile = BowingProfile.Small,
        strongBeat: (Double) -> Boolean = { false },
    ): BowingPlan {
        val strokes = segment(toEvents(notes), profile)
        return BowingPlan(assign(strokes, profile, strongBeat))
    }

    private fun toEvents(notes: List<BowNote>): List<BowEvent> {
        val events = mutableListOf<BowEvent>()
        for (note in notes.sortedBy { it.start }) {
            val last = events.lastOrNull()
            if (last != null && abs(note.start - last.start) <= DOUBLE_STOP_WINDOW) {
                events[events.lastIndex] = last.copy(
                    end = max(last.end, note.end),
                    strings = last.strings + note.string,
                    velocity = max(last.velocity, note.velocity),
                )
            } else {
                events += BowEvent(note.start, note.end, setOf(note.string), note.velocity)
            }
        }
        // A note can't outlast the next onset on the same bow.
        return events.mapIndexed { i, e ->
            val next = events.getOrNull(i + 1)
            if (next != null && e.end > next.start) e.copy(end = max(next.start, e.start + 1e-3)) else e
        }
    }

    private fun segment(events: List<BowEvent>, profile: BowingProfile): List<List<BowEvent>> {
        val strokes = mutableListOf<MutableList<BowEvent>>()
        for (event in events) {
            val current = strokes.lastOrNull()
            val last = current?.last()
            val slurs = last != null &&
                    event.start - last.end <= profile.slurGap &&
                    event.end - event.start >= profile.shortNote &&
                    last.end - last.start >= profile.shortNote &&
                    last.strings != event.strings &&
                    event.end - current.first().start <= profile.maxSlur
            if (slurs) current.add(event) else strokes += mutableListOf(event)
        }
        return strokes
    }

    private data class State(
        val direction: BowDirection,
        val pos: Double,
        val cost: Double,
        val path: List<BowStroke>,
    )

    private fun assign(groups: List<List<BowEvent>>, profile: BowingProfile, strongBeat: (Double) -> Boolean): List<BowStroke> {
        if (groups.isEmpty()) return emptyList()
        var beam = listOf(State(BowDirection.Up, 0.0, 0.0, emptyList()))
        var previousEnd = Double.NEGATIVE_INFINITY
        for ((index, group) in groups.withIndex()) {
            val start = group.first().start
            val end = group.last().end
            val gap = start - previousEnd
            val next = mutableListOf<State>()
            for (state in beam) {
                for (direction in BowDirection.entries) {
                    var cost = state.cost
                    val retake = index == 0 || state.direction == direction
                    val startPos = when {
                        !retake -> state.pos
                        direction == BowDirection.Down -> 0.0
                        else -> 1.0
                    }
                    if (retake && index > 0) cost += if (gap >= profile.retakeGap) RETAKE_COST else NO_TIME_FOR_RETAKE_COST
                    if (direction == BowDirection.Up && strongBeat(start)) cost += WRONG_BEAT_COST
                    // Saturating: a short note still sweeps a good part of the bow, and a long one approaches all of it.
                    val wanted = profile.minTravel + (1 - profile.minTravel) * (1 - exp(-(end - start) / profile.fullBowTime))
                    val available = if (direction == BowDirection.Down) 1 - startPos else startPos
                    val travel = min(wanted, available)
                    if (travel < wanted * 0.6) cost += RUNNING_OUT_COST * (1 - travel / wanted)
                    val endPos = if (direction == BowDirection.Down) startPos + travel else startPos - travel
                    val stroke = BowStroke(start, end, direction, startPos, endPos, retake && index > 0, group)
                    next += State(direction, endPos, cost, state.path + stroke)
                }
            }
            beam = next.sortedBy { it.cost }.take(BEAM_WIDTH)
            previousEnd = end
        }
        return beam.first().path
    }
}
