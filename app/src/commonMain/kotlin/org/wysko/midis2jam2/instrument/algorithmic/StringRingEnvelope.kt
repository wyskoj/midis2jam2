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

import kotlin.math.pow
import kotlin.time.Duration

/** Below this, a ringing string is taken to have stopped. */
private const val SILENCE = 0.01

/**
 * How strongly a string vibrates: fully while its note is held, then dying away once it's let go, halving every
 * [halfLife].
 */
class StringRingEnvelope(private val halfLife: Duration) {

    /** How strongly the string vibrates, from 0 (still) to 1. */
    var amplitude: Double = 0.0
        private set

    /** Whether the string is vibrating at all. */
    val isRinging: Boolean
        get() = amplitude > 0.0

    /**
     * Update the envelope.
     *
     * @param delta the amount of time since the last frame update
     * @param held whether the string's note is held
     * @return the new [amplitude]
     */
    fun tick(delta: Duration, held: Boolean): Double {
        amplitude = when {
            held -> 1.0
            else -> (amplitude * 0.5.pow(delta / halfLife)).takeIf { it >= SILENCE } ?: 0.0
        }
        return amplitude
    }
}
