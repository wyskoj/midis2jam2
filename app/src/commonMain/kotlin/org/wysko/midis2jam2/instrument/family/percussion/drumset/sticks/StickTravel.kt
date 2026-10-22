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

import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletKeyframe
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPath
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Keeps a stick from drifting across a long gap between strikes. A [MalletPath] spreads each move over the whole gap,
 * which on a long one has the stick creeping toward the next piece for seconds. Instead, the stick stays on the piece
 * it last struck until it has just enough time to travel to the next one at [speed], then makes the same move in that
 * time. The path is left as it is; this only says how far along it the stick is at a given moment ([pathTime]).
 *
 * @param keyframes the stick's strikes, in time order, as given to its [MalletPath].
 * @param speed how fast the stick travels between pieces, in the kit's units per second.
 * @param minTravel the least time a move takes, however short, in seconds.
 */
class StickTravel(
    private val keyframes: List<MalletKeyframe>,
    speed: Double,
    minTravel: Double,
) {
    /** For each gap between strikes, when the stick is over the next piece, ready to wind up (as the path has it). */
    private val arrives = keyframes.zipWithNext { a, b -> MalletPath.defaultTiming(a, b).arrive }

    /** For each gap between strikes, when the stick sets off for the next piece. */
    private val departs = keyframes.zipWithNext().mapIndexed { i, (a, b) ->
        val travel = max(distance(a, b) / speed, minTravel)
        max(a.time, arrives[i] - travel)
    }

    /** The time to look up in the stick's [MalletPath] for where the stick is at [time]. */
    fun pathTime(time: Double): Double {
        val i = MalletPath.segmentAt(keyframes, time) ?: return time
        val start = keyframes[i].time
        val depart = departs[i]
        val arrive = arrives[i]
        return when {
            time >= arrive || depart <= start -> time
            time <= depart -> start
            else -> start + (time - depart) / (arrive - depart) * (arrive - start)
        }
    }

    private fun distance(a: MalletKeyframe, b: MalletKeyframe): Double {
        val dx = a.point.x - b.point.x
        val dy = a.point.y - b.point.y
        val dz = a.point.z - b.point.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
