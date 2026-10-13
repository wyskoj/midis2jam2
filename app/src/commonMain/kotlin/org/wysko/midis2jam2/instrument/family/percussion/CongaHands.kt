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
package org.wysko.midis2jam2.instrument.family.percussion

import kotlin.math.max

/** The three ways a conga is struck. */
internal enum class CongaStroke {
    /** Open stroke on the high conga, always played by the left hand. */
    OPEN_HIGH,

    /** Muted stroke, played by either hand on its own conga. */
    MUTE,

    /** Open stroke on the low conga, always played by the right hand. */
    LOW,
}

/** The hands playing the congas. The left hand plays the high conga, the right hand the low one. */
internal enum class CongaHand { LEFT, RIGHT }

/** A conga hit, at [time] seconds. */
internal data class CongaHit(val time: Double, val stroke: CongaStroke)

/**
 * Decides which hand plays each conga hit. Open strokes are fixed to their conga's hand; a mute can be played by
 * either hand, by moving it to the mute position on its own conga.
 *
 * A beam search over the hits, in time order, finds the cheapest assignment. A hand is penalised for striking soon
 * after its last strike, more so if it has to move between the open and mute positions in between, and for striking
 * twice at once. Fast passages therefore alternate hands, and a mute played alongside an open high stroke goes to the
 * right hand. Mutes the left hand can play comfortably stay on the high conga, where they traditionally go.
 */
internal object CongaHands {
    private const val BEAM_WIDTH = 64

    /** Below this gap (seconds), two strikes by one hand count as simultaneous. */
    private const val SIMULTANEOUS = 0.02

    /** Cost of one hand striking twice at once: effectively forbidden. */
    private const val COLLISION_COST = 1000.0

    /** Cost per strike of a hand striking again, divided by the gap since its last strike. */
    private const val REPEAT_WEIGHT = 0.1

    /** Extra cost of moving between the open and mute positions, divided by the time there is to do it. */
    private const val MOVE_WEIGHT = 0.15

    /** Small preference for playing mutes on the high conga. */
    private const val RIGHT_MUTE_COST = 0.2

    private class State(
        val lastLeft: Int,
        val lastRight: Int,
        val cost: Double,
        val hand: CongaHand?,
        val parent: State?,
    )

    /** Returns the hand that plays each of [hits], in the same order. */
    fun assign(hits: List<CongaHit>): List<CongaHand> {
        if (hits.isEmpty()) return emptyList()
        val order = hits.indices.sortedBy { hits[it].time }
        val sorted = order.map { hits[it] }

        var beam = listOf(State(-1, -1, 0.0, null, null))
        for ((i, hit) in sorted.withIndex()) {
            val hands = when (hit.stroke) {
                CongaStroke.OPEN_HIGH -> listOf(CongaHand.LEFT)
                CongaStroke.LOW -> listOf(CongaHand.RIGHT)
                CongaStroke.MUTE -> CongaHand.entries
            }
            val next = HashMap<Long, State>()
            for (state in beam) {
                for (hand in hands) {
                    val last = if (hand == CongaHand.LEFT) state.lastLeft else state.lastRight
                    val cost = state.cost + strikeCost(sorted, last, i, hand)
                    val candidate = State(
                        lastLeft = if (hand == CongaHand.LEFT) i else state.lastLeft,
                        lastRight = if (hand == CongaHand.RIGHT) i else state.lastRight,
                        cost = cost,
                        hand = hand,
                        parent = state,
                    )
                    val key = (candidate.lastLeft.toLong() shl 32) or (candidate.lastRight.toLong() and 0xFFFFFFFFL)
                    val existing = next[key]
                    if (existing == null || cost < existing.cost) next[key] = candidate
                }
            }
            beam = next.values.sortedBy { it.cost }.take(BEAM_WIDTH)
        }

        val sortedHands = generateSequence(beam.first()) { it.parent }.mapNotNull { it.hand }.toList().asReversed()
        val result = arrayOfNulls<CongaHand>(hits.size)
        order.forEachIndexed { sortedIndex, original -> result[original] = sortedHands[sortedIndex] }
        return result.map { it!! }
    }

    /** How long, at most, a hand takes to move between the open and mute positions (seconds). */
    private const val MOVE_TIME = 0.3

    /**
     * Where a hand is between its open (0) and mute (1) positions at [time]. It stays where it last struck, then
     * moves to where it next strikes, arriving a little before the strike.
     *
     * @param last the time and whether it was a mute, of the hand's last strike, if any
     * @param next the time and whether it is a mute, of the hand's next strike, if any
     */
    fun muteBlend(time: Double, last: Pair<Double, Boolean>?, next: Pair<Double, Boolean>?): Double {
        val from = last?.second?.toBlend()
        val to = next?.second?.toBlend() ?: return from ?: 0.0
        if (from == null || last.first >= next.first) return to
        val start = max(last.first, next.first - MOVE_TIME)
        val end = start + (next.first - start) * ARRIVE_FRACTION
        val t = ((time - start) / (end - start)).coerceIn(0.0, 1.0)
        return from + (to - from) * t * t * (3 - 2 * t)
    }

    /** How far through the time before a strike a hand arrives at the strike's position. */
    private const val ARRIVE_FRACTION = 0.8

    private fun Boolean.toBlend() = if (this) 1.0 else 0.0

    private fun strikeCost(hits: List<CongaHit>, last: Int, current: Int, hand: CongaHand): Double {
        val hit = hits[current]
        var cost = if (hit.stroke == CongaStroke.MUTE && hand == CongaHand.RIGHT) RIGHT_MUTE_COST else 0.0
        if (last < 0) return cost

        val previous = hits[last]
        val gap = hit.time - previous.time
        if (gap < SIMULTANEOUS) cost += COLLISION_COST
        val time = max(gap, SIMULTANEOUS)
        cost += REPEAT_WEIGHT / time
        if ((previous.stroke == CongaStroke.MUTE) != (hit.stroke == CongaStroke.MUTE)) cost += MOVE_WEIGHT / time
        return cost
    }
}
