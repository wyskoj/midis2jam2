/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.util

import kotlin.math.abs

/** Applies smoothing to a given value by slowing encroaching on that value over time. */
class NumberSmoother(initialValue: Float, private val smoothness: Double) {

    /**
     * The current smoothed value.
     */
    var value: Float = initialValue
        private set

    /**
     * Call this on every frame to update the new smoothed value.
     *
     * @param delta the amount of time that has passed since the last frame
     * @param target the target value
     * @return [value]
     */
    fun tick(delta: Float, target: () -> Float): Float {
        when (smoothness) {
            0.0 -> value = target.invoke()
            else -> with(target.invoke()) {
                value += ((this - value) * delta * smoothness).toFloat().coerceIn(
                    -abs(this - value)..abs(this - value) // Prevent the change from overshooting
                )
            }
        }
        return value
    }
}