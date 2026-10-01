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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks

import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Somewhere on the kit a stick strikes.
 *
 * @property id identifies the target (one piece, or one spot on a piece like the ride's bell).
 * @property position where the stick strikes, in the kit's space.
 * @property profile which hand prefers it.
 */
data class DrumTarget(val id: String, val position: Point3, val profile: HandProfile)

/**
 * One hit a stick has to play.
 *
 * @property time when it sounds, in seconds.
 * @property target where on the kit.
 * @property source the caller's payload, usually the NoteOn event.
 */
data class StickHit<T>(val time: Double, val target: DrumTarget, val source: T)

/**
 * Tuning weights for [DrumStickPlanner]. Distances are in the kit's units (the snare to the hi-hat is about 10) and
 * times in seconds.
 *
 * @property chordWindow hits closer together than this are struck together.
 * @property beamWidth how many candidate plans survive each chord.
 * @property travelWeight cost per unit of distance a stick moves.
 * @property maxSpeed the fastest a stick moves before the plan is penalized for it.
 * @property speedWeight cost per unit of speed above [maxSpeed], squared.
 * @property alternateBelow a hand striking again sooner than this is penalized, so rolls and fills alternate hands.
 * @property repeatWeight the cost of an instant repeat by the same hand, fading to nothing at [alternateBelow].
 * @property handWeight how much each piece's [HandProfile] costs count.
 */
data class DrumStickParams(
    val chordWindow: Double = 0.015,
    val beamWidth: Int = 64,
    val travelWeight: Double = 0.1,
    val maxSpeed: Double = 300.0,
    val speedWeight: Double = 0.0005,
    val alternateBelow: Double = 0.2,
    val repeatWeight: Double = 6.0,
    val handWeight: Double = 1.0,
)

/**
 * Which hand strikes which hit.
 *
 * @property perHand the hits each hand strikes, in time order: [HandProfile.LEFT] first, then [HandProfile.RIGHT].
 * @property unassigned hits no stick strikes, because more pieces were struck at once than there are hands. The
 * pieces still react to them.
 */
data class StickPlan<T>(val perHand: List<List<StickHit<T>>>, val unassigned: List<StickHit<T>>)

/**
 * Decides which of a drummer's two sticks strikes each hit, for a right-handed drummer playing crossed.
 *
 * Hits struck together are grouped into chords; if a chord has more hits than there are hands, the highest
 * [HandProfile.priority] ones are kept. A beam search then walks the chords in order, trying both ways of handing a
 * two-hit chord to the hands (or either hand for one hit), and keeps the cheapest plans. The cost prefers short,
 * unhurried moves, alternating hands on fast hits, and each hand playing the pieces it prefers (the right hand on the
 * hi-hat, ride and floor toms, the left on the snare). Unlike mallets on a keyboard, sticks may cross freely.
 */
object DrumStickPlanner {

    /** The number of hands. */
    private const val HANDS = 2

    /** Plans [hits], starting with the hands over [home] (left first). */
    fun <T> plan(
        hits: List<StickHit<T>>,
        home: List<Point3>,
        params: DrumStickParams = DrumStickParams(),
    ): StickPlan<T> {
        require(home.size == HANDS) { "Need a home position for each of the $HANDS hands" }
        if (hits.isEmpty()) return StickPlan(List(HANDS) { emptyList() }, emptyList())

        val unassigned = mutableListOf<StickHit<T>>()
        val chords = chordsOf(hits.sortedBy { it.time }, params.chordWindow).map { chord ->
            // A piece struck twice at once is struck once; then the most important pieces get the sticks.
            val kept = chord.distinctBy { it.target.id }
                .sortedWith(compareByDescending<StickHit<T>> { it.target.profile.priority }.thenBy { it.target.id })
                .take(HANDS)
            unassigned += chord.filter { hit -> kept.none { it === hit } }
            kept
        }

        var beam = listOf(
            State(
                parent = null,
                chordIndex = -1,
                assignment = IntArray(0),
                positions = home.toTypedArray(),
                lastHit = DoubleArray(HANDS) { Double.NEGATIVE_INFINITY },
                cost = 0.0,
            )
        )

        chords.forEachIndexed { chordIndex, chord ->
            val time = chord.first().time
            val next = HashMap<String, State>()
            for (state in beam) {
                for (assignment in assignments(chord.size)) {
                    val candidate = extend(state, chordIndex, assignment, chord, time, params)
                    val key = candidate.positions.joinToString(";") { it.key() }
                    val existing = next[key]
                    if (existing == null || candidate.cost < existing.cost) next[key] = candidate
                }
            }
            beam = next.values.sortedBy { it.cost }.take(params.beamWidth)
        }

        val perHand = List(HANDS) { mutableListOf<StickHit<T>>() }
        generateSequence(beam.minBy { it.cost }) { it.parent }.forEach { state ->
            if (state.chordIndex < 0) return@forEach
            chords[state.chordIndex].forEachIndexed { i, hit -> perHand[state.assignment[i]] += hit }
        }
        perHand.forEach { it.sortBy { hit -> hit.time } }

        return StickPlan(perHand, unassigned.sortedBy { it.time })
    }

    private class State(
        val parent: State?,
        val chordIndex: Int,
        val assignment: IntArray,
        val positions: Array<Point3>,
        val lastHit: DoubleArray,
        val cost: Double,
    )

    private fun <T> extend(
        state: State,
        chordIndex: Int,
        assignment: IntArray,
        chord: List<StickHit<T>>,
        time: Double,
        params: DrumStickParams,
    ): State {
        val positions = state.positions.copyOf()
        val lastHit = state.lastHit.copyOf()
        var cost = state.cost

        assignment.forEachIndexed { i, hand ->
            val target = chord[i].target
            val distance = distance(positions[hand], target.position)
            val elapsed = time - lastHit[hand]

            cost += params.travelWeight * distance
            cost += params.handWeight * target.profile.costFor(hand)

            val excess = max(0.0, distance / max(elapsed, 1e-3) - params.maxSpeed)
            cost += params.speedWeight * excess * excess

            if (elapsed < params.alternateBelow) {
                cost += params.repeatWeight * (1 - elapsed / params.alternateBelow)
            }

            positions[hand] = target.position
            lastHit[hand] = time
        }

        return State(state, chordIndex, assignment, positions, lastHit, cost)
    }

    /** Every way of giving [size] hits to distinct hands: hit `i` goes to hand `assignment[i]`. */
    private fun assignments(size: Int): List<IntArray> = when (size) {
        1 -> listOf(intArrayOf(HandProfile.LEFT), intArrayOf(HandProfile.RIGHT))
        2 -> listOf(intArrayOf(HandProfile.LEFT, HandProfile.RIGHT), intArrayOf(HandProfile.RIGHT, HandProfile.LEFT))
        else -> error("A chord has at most $HANDS hits once reduced, got $size")
    }

    /** Groups [sorted] hits into chords of hits within [window] of the chord's first hit. */
    private fun <T> chordsOf(sorted: List<StickHit<T>>, window: Double): List<List<StickHit<T>>> {
        val chords = mutableListOf<MutableList<StickHit<T>>>()
        for (hit in sorted) {
            val current = chords.lastOrNull()
            if (current != null && hit.time - current.first().time <= window) current += hit else chords += mutableListOf(hit)
        }
        return chords
    }

    private fun distance(a: Point3, b: Point3): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun Point3.key(): String = "${(x * 10).roundToInt()},${(y * 10).roundToInt()},${(z * 10).roundToInt()}"
}
