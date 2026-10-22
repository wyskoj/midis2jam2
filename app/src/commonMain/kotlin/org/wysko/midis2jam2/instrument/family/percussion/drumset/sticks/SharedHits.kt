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

/**
 * Finds the hits each hand shares a piece with the other hand on: where the other hand strikes the same target within
 * [window] seconds, before or after, as in a roll or a fill that alternates hands on one drum. Two sticks there would
 * strike the same spot, one on top of the other, so these are the hits a stick moves aside for.
 *
 * @param perHand each hand's hits, as [StickPlan.perHand] gives them.
 * @return for each hand, whether each of its hits is shared, in the same order.
 */
fun <T> sharedHits(perHand: List<List<StickHit<T>>>, window: Double): List<List<Boolean>> {
    val timesByTarget = perHand.map { hits ->
        hits.groupBy({ it.target.id }, { it.time }).mapValues { (_, times) -> times.sorted().toDoubleArray() }
    }
    return perHand.mapIndexed { hand, hits ->
        val others = timesByTarget.filterIndexed { other, _ -> other != hand }
        hits.map { hit -> others.any { it[hit.target.id]?.hasWithin(hit.time, window) == true } }
    }
}

/** Whether any of these times, which are sorted, is within [window] of [time]. */
private fun DoubleArray.hasWithin(time: Double, window: Double): Boolean {
    val i = binarySearch(time).let { if (it < 0) -it - 1 else it }
    return (i < size && this[i] - time <= window) || (i > 0 && time - this[i - 1] <= window)
}
