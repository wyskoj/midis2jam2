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
import kotlin.math.roundToInt

/**
 * One note a mallet instrument has to play.
 *
 * @param T whatever the caller wants carried through the plan, usually the NoteOn event.
 * @property time when the note sounds, in seconds.
 * @property note the MIDI note number.
 * @property source the caller's payload.
 */
data class MalletHit<T>(val time: Double, val note: Int, val source: T)

/**
 * Tuning weights for [MalletPlanner]. Distances are in the instrument's horizontal units (one white bar is about
 * 1.33 wide) and times in seconds.
 *
 * @property chordWindow onsets closer together than this are struck together.
 * @property beamWidth how many candidate plans survive each chord.
 * @property travelWeight cost per unit of distance a mallet moves.
 * @property maxSpeed the fastest a mallet moves before the plan is penalized for it.
 * @property speedWeight cost per unit of speed above [maxSpeed], squared.
 * @property alternateBelow a mallet striking again sooner than this is penalized, so fast notes alternate hands.
 * @property repeatWeight the cost of an instant repeat by the same mallet, fading to nothing at [alternateBelow].
 * @property homeWeight cost per unit of distance a mallet strikes from its home position.
 * @property maxCross the furthest one mallet may be past another (on the wrong side of it) at any time.
 * @property escapeCost the cost of a mallet striking past another that then gets out of its way by moving on to a
 * note on its own side. No mallet passes over another; they just keep out of each other's way.
 * @property crossBase the cost of a real crossover, where one mallet has to pass over another.
 * @property crossWeight the extra cost of a real crossover per unit of distance the mallets overlap.
 * @property crossPersistWeight the cost per chord, per unit of overlap, of mallets staying crossed.
 */
data class MalletCostParams(
    val chordWindow: Double = 0.015,
    val beamWidth: Int = 64,
    val travelWeight: Double = 1.0,
    val maxSpeed: Double = 80.0,
    val speedWeight: Double = 0.02,
    val alternateBelow: Double = 0.3,
    val repeatWeight: Double = 12.0,
    val homeWeight: Double = 0.1,
    val maxCross: Double = 3.0,
    val escapeCost: Double = 0.3,
    val crossBase: Double = 4.0,
    val crossWeight: Double = 2.0,
    val crossPersistWeight: Double = 1.0,
)

/**
 * Which mallet strikes which note.
 *
 * @property perMallet for each mallet, left to right, the hits it strikes in time order.
 * @property unassigned hits no mallet strikes: the notes of a chord bigger than the number of mallets, repeated notes
 * within a chord, or (rarely) chords the mallets can't reach without crossing too far.
 */
data class MalletPlan<T>(val perMallet: List<List<MalletHit<T>>>, val unassigned: List<MalletHit<T>>)

/**
 * Decides which of a limited number of mallets strikes each note, the way a player with two (or four) mallets would.
 *
 * Notes that sound together are grouped into chords. A beam search then walks the chords in order, trying every way
 * of handing a chord's notes to distinct mallets (in left-to-right order within the chord), and keeps the cheapest
 * plans. The cost prefers short, unhurried moves, alternating mallets on fast notes, and each mallet staying near its
 * own part of the keyboard.
 *
 * Mallets may cross, a little. When a mallet strikes past another, the crossing is "pending" until one of them moves
 * again. If the other mallet then moves on to a note on its own side, it only had to get out of the way (an escape,
 * like the left hand leading a descending run while the right hand follows), and that's cheap. Otherwise the mallets
 * really crossed over, which costs more, and more for every chord they stay crossed. [MalletChoreographer] later
 * works out the timing that lets an escape happen without the mallets touching, and lifts one mallet over the other
 * for a real crossover.
 */
object MalletPlanner {

    /**
     * Plans [hits] for [malletCount] mallets, where [x] gives the horizontal position of a note's bar.
     */
    fun <T> plan(
        hits: List<MalletHit<T>>,
        malletCount: Int,
        x: (note: Int) -> Double,
        params: MalletCostParams = MalletCostParams(),
    ): MalletPlan<T> {
        require(malletCount >= 1) { "At least one mallet is needed, got $malletCount" }
        if (hits.isEmpty()) return MalletPlan(List(malletCount) { emptyList() }, emptyList())

        val unassigned = mutableListOf<MalletHit<T>>()
        val chords = chordsOf(hits.sortedBy { it.time }, params.chordWindow).map { chord ->
            val (playable, extra) = reduce(chord, malletCount)
            unassigned += extra
            playable.sortedWith(compareBy({ x(it.note) }, { it.note }))
        }

        val xs = hits.map { x(it.note) }
        val low = xs.min()
        val high = xs.max()
        val home = DoubleArray(malletCount) { low + (it + 0.5) / malletCount * (high - low) }

        var beam = listOf(
            State(
                parent = null,
                chordIndex = -1,
                assignment = IntArray(0),
                positions = home.copyOf(),
                lastHit = DoubleArray(malletCount) { Double.NEGATIVE_INFINITY },
                crossings = IntArray(malletCount * (malletCount - 1) / 2),
                cost = 0.0,
            )
        )

        chords.forEachIndexed { chordIndex, chord ->
            val noteXs = chord.map { x(it.note) }
            val time = chord.first().time
            val combinations = combinations(malletCount, chord.size)

            val next = HashMap<String, State>()
            for (state in beam) {
                for (combination in combinations) {
                    val candidate = extend(state, chordIndex, combination, noteXs, time, home, params) ?: continue
                    val key = candidate.positions.joinToString(",") { (it * 100).roundToInt().toString() } +
                        candidate.crossings.joinToString(",", prefix = "|")
                    val existing = next[key]
                    if (existing == null || candidate.cost < existing.cost) next[key] = candidate
                }
            }

            if (next.isEmpty()) {
                // No way to strike this chord without crossing too far; let the bars ring on their own.
                unassigned += chord
            } else {
                beam = next.values.sortedBy { it.cost }.take(params.beamWidth)
            }
        }

        val perMallet = List(malletCount) { mutableListOf<MalletHit<T>>() }
        // A crossing still pending at the end never got resolved by an escape, so it was a real crossover.
        val best = beam.minBy { state -> state.cost + unresolvedCrossingCost(state, params) }
        generateSequence(best) { it.parent }.forEach { state ->
            if (state.chordIndex < 0) return@forEach
            chords[state.chordIndex].forEachIndexed { i, hit -> perMallet[state.assignment[i]] += hit }
        }
        perMallet.forEach { it.sortBy { hit -> hit.time } }

        return MalletPlan(perMallet, unassigned.sortedBy { it.time })
    }

    /**
     * One partial plan.
     *
     * @property crossings for each pair of mallets (see [forEachPair]), [NOT_CROSSED], [PENDING_LEFT_BLOCKS],
     * [PENDING_RIGHT_BLOCKS] or [CROSSED].
     */
    private class State(
        val parent: State?,
        val chordIndex: Int,
        val assignment: IntArray,
        val positions: DoubleArray,
        val lastHit: DoubleArray,
        val crossings: IntArray,
        val cost: Double,
    )

    /** The pair is in order. */
    private const val NOT_CROSSED = 0

    /** The right mallet struck past the left one; if the left one moves on to its own side, it was an escape. */
    private const val PENDING_LEFT_BLOCKS = 1

    /** The left mallet struck past the right one; if the right one moves on to its own side, it was an escape. */
    private const val PENDING_RIGHT_BLOCKS = 2

    /** The pair really crossed over, and still is. */
    private const val CROSSED = 3

    /** Calls [action] with an index for every pair of mallets, left (lower index) first. */
    private inline fun forEachPair(malletCount: Int, action: (pair: Int, left: Int, right: Int) -> Unit) {
        var pair = 0
        for (left in 0 until malletCount) {
            for (right in left + 1 until malletCount) action(pair++, left, right)
        }
    }

    private fun realCrossingCost(overlap: Double, params: MalletCostParams): Double =
        params.crossBase + params.crossWeight * overlap

    private fun unresolvedCrossingCost(state: State, params: MalletCostParams): Double {
        var cost = 0.0
        forEachPair(state.positions.size) { pair, left, right ->
            val status = state.crossings[pair]
            if (status == PENDING_LEFT_BLOCKS || status == PENDING_RIGHT_BLOCKS) {
                cost += realCrossingCost(state.positions[left] - state.positions[right], params)
            }
        }
        return cost
    }

    private fun extend(
        state: State,
        chordIndex: Int,
        combination: IntArray,
        noteXs: List<Double>,
        time: Double,
        home: DoubleArray,
        params: MalletCostParams,
    ): State? {
        val positions = state.positions.copyOf()
        val lastHit = state.lastHit.copyOf()
        var cost = state.cost

        combination.forEachIndexed { i, mallet ->
            val target = noteXs[i]
            val distance = abs(target - positions[mallet])
            val elapsed = time - lastHit[mallet]

            cost += params.travelWeight * distance
            cost += params.homeWeight * abs(target - home[mallet])

            val speed = distance / max(elapsed, 1e-3)
            val excess = max(0.0, speed - params.maxSpeed)
            cost += params.speedWeight * excess * excess

            if (elapsed < params.alternateBelow) {
                cost += params.repeatWeight * (1 - elapsed / params.alternateBelow)
            }

            positions[mallet] = target
            lastHit[mallet] = time
        }

        val moved = BooleanArray(positions.size).also { moved -> combination.forEach { moved[it] = true } }
        val crossings = state.crossings.copyOf()
        var tooFar = false

        // Two mallets may share a bar (a player alternates on one bar with both hands); that isn't a crossing.
        forEachPair(positions.size) { pair, left, right ->
            val overlap = positions[left] - positions[right]
            val wasOverlap = state.positions[left] - state.positions[right]
            if (overlap > params.maxCross) tooFar = true

            val before = crossings[pair]
            val crossedNow = overlap > 1e-9
            val pending = before == PENDING_LEFT_BLOCKS || before == PENDING_RIGHT_BLOCKS
            val blocker = if (before == PENDING_LEFT_BLOCKS) left else right

            crossings[pair] = when {
                // A new crossing: whoever didn't move is in the way, for now.
                crossedNow && before == NOT_CROSSED -> when {
                    moved[right] && !moved[left] -> PENDING_LEFT_BLOCKS
                    moved[left] && !moved[right] -> PENDING_RIGHT_BLOCKS
                    else -> CROSSED.also { cost += realCrossingCost(overlap, params) }
                }

                // Still waiting to see whether the mallet in the way escapes.
                crossedNow && pending && !moved[left] && !moved[right] -> before

                // One of them moved and they're still crossed: that's a real crossover.
                crossedNow && pending -> CROSSED.also { cost += realCrossingCost(overlap, params) }

                crossedNow -> CROSSED.also { cost += params.crossPersistWeight * overlap }

                // Uncrossed. If the mallet in the way moved to its own side, it only had to get out of the way.
                pending && moved[blocker] -> NOT_CROSSED.also { cost += params.escapeCost }
                pending -> NOT_CROSSED.also { cost += realCrossingCost(wasOverlap, params) }

                else -> NOT_CROSSED
            }
        }
        if (tooFar) return null

        return State(state, chordIndex, combination, positions, lastHit, crossings, cost)
    }

    /** Groups [sorted] hits into chords of onsets within [window] of the chord's first note. */
    private fun <T> chordsOf(sorted: List<MalletHit<T>>, window: Double): List<List<MalletHit<T>>> {
        val chords = mutableListOf<MutableList<MalletHit<T>>>()
        for (hit in sorted) {
            val current = chords.lastOrNull()
            if (current != null && hit.time - current.first().time <= window) current += hit else chords += mutableListOf(hit)
        }
        return chords
    }

    /**
     * Splits a chord into the notes the mallets strike and the ones they can't. A note repeated within the chord is
     * struck once. If there are more notes than mallets, the outermost notes are kept and the rest spread evenly.
     */
    private fun <T> reduce(chord: List<MalletHit<T>>, malletCount: Int): Pair<List<MalletHit<T>>, List<MalletHit<T>>> {
        val distinct = chord.distinctBy { it.note }.sortedBy { it.note }
        val repeats = chord.filter { hit -> distinct.none { it === hit } }
        if (distinct.size <= malletCount) return distinct to repeats

        val keep = if (malletCount == 1) {
            setOf(distinct.lastIndex)
        } else {
            (0 until malletCount).map { (it * distinct.lastIndex.toDouble() / (malletCount - 1)).roundToInt() }.toSet()
        }
        val kept = distinct.filterIndexed { i, _ -> i in keep }
        return kept to (repeats + distinct.filterIndexed { i, _ -> i !in keep })
    }

    /** Every increasing choice of [k] mallets out of [n]. */
    private fun combinations(n: Int, k: Int): List<IntArray> {
        val result = mutableListOf<IntArray>()
        fun build(start: Int, chosen: List<Int>) {
            if (chosen.size == k) {
                result += chosen.toIntArray()
                return
            }
            for (i in start until n) build(i + 1, chosen + i)
        }
        build(0, emptyList())
        return result
    }
}
