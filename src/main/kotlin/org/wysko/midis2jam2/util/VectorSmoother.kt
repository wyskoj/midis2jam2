/*
 * Copyright (C) 2024 Jacob Wysko
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

import com.jme3.math.Vector3f
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * Applies smoothing to a given vector by slowly moving to that vector over time.
 *
 * @param initialValue The initial value of the vector.
 * @param smoothness The smoothness of the vector.
 */
class VectorSmoother(initialValue: Vector3f, private val smoothness: Double) {

    /**
     * The current smoothed vector.
     */
    var value: Vector3f = initialValue.clone()

    /**
     * Call this on every frame to update the new smoothed vector.
     *
     * @param delta the amount of time that has passed since the last frame
     * @param target the target vector
     * @return [value]
     */
    fun tick(delta: Duration, target: () -> Vector3f): Vector3f {
        when (smoothness) {
            0.0 -> value = target.invoke()
            else -> with(target.invoke()) {
                value.addLocal(
                    this.subtract(value).multLocal((delta.toDouble(SECONDS) * smoothness).toFloat())
                )
            }
        }
        return value
    }
}