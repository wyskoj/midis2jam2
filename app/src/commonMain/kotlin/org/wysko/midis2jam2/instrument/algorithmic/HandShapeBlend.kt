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
package org.wysko.midis2jam2.instrument.algorithmic

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Times the move of a hand from one fingering shape to the next.
 *
 * The fingers move ahead of the next note and arrive exactly as it starts, so the shape is always right while a note
 * sounds. The move takes up to [MAX_TRANSITION], drawn from the rest before the next note and the tail of the previous
 * one (at most [PREVIOUS_NOTE_SHARE] of it), so legato passages get a quick move and slow ones a relaxed one.
 */
object HandShapeBlend {

    /** The longest a move between two shapes takes. */
    val MAX_TRANSITION: Duration = 90.milliseconds

    /** How much of the previous note the move may take, when there's no rest before the next note. */
    const val PREVIOUS_NOTE_SHARE: Double = 0.3

    /**
     * A hand partway between two shapes.
     *
     * @property from The shape the hand is leaving.
     * @property to The shape the hand is moving to.
     * @property t How far along the move is, eased, from 0 (at [from]) to 1 (at [to]).
     */
    data class Blend(val from: Int, val to: Int, val t: Float) {
        override fun toString(): String = if (from == to || t == 0f) "$from" else "$from→$to @${(t * 100).roundToInt() / 100f}"
    }

    /**
     * How far the hand has moved toward the shape of the note starting at [nextStart], at [time]: 0 before the move
     * starts, 1 from the note's start on, eased in between. [previousStart] and [previousEnd] are the note before it,
     * or null if there isn't one.
     */
    fun progress(time: Duration, nextStart: Duration, previousStart: Duration?, previousEnd: Duration?): Float {
        val window = transition(nextStart, previousStart, previousEnd)
        if (window <= Duration.ZERO) return if (time >= nextStart) 1f else 0f
        val linear = ((time - (nextStart - window)) / window).coerceIn(0.0, 1.0).toFloat()
        return linear * linear * (3 - 2 * linear)
    }

    /** How long the move into the note starting at [nextStart] takes. */
    fun transition(nextStart: Duration, previousStart: Duration?, previousEnd: Duration?): Duration {
        if (previousStart == null || previousEnd == null) return MAX_TRANSITION
        val rest = (nextStart - previousEnd).coerceAtLeast(Duration.ZERO)
        val tail = (previousEnd - previousStart).coerceAtLeast(Duration.ZERO) * PREVIOUS_NOTE_SHARE
        return minOf(MAX_TRANSITION, rest + tail, (nextStart - previousStart).coerceAtLeast(Duration.ZERO))
    }
}
