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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.FastMath.PI
import com.jme3.math.Quaternion
import com.jme3.scene.Spatial
import kotlin.math.cos
import kotlin.math.pow

/** Animates the wobble on cymbals using [a sinusoidal function](https://www.desmos.com/calculator/vvbwlit9he). */
class CymbalAnimator(
    /** The cymbal to animate. */
    private val cymbal: Spatial,
    /** The amplitude, or maximum angle of wobble. */
    private val amplitude: Double,
    /** How fast the cymbal wobbles after being struck. */
    private val wobbleSpeed: Double,
    /** The dampening, or how fast the cymbal returns to an idle state. */
    private val dampening: Double,
    private val phaseOffset: Float = 0.0f
) {

    /** The current time. */
    var animTime: Double = -1.0
        private set

    /**
     * Calculates and returns the wobble angle, based on the [animTime].
     */
    private fun rotationAmount(): Float =
        when {
            animTime < 0 -> 0f
            else -> (amplitude * (cos(animTime * wobbleSpeed * PI + phaseOffset) / (3 + animTime.pow(3) * wobbleSpeed * dampening * PI))).toFloat()
        }

    /** Call this method to indicate that the cymbal has just been struck. */
    fun strike() {
        animTime = 0.0
    }

    /**
     * Updates the internal clock for proper animation.
     *
     * @param delta the amount of time since the last frame
     */
    fun tick(delta: Float) {
        if (animTime != -1.0) {
            animTime += delta.toDouble()
        }
        cymbal.localRotation = Quaternion().fromAngles(rotationAmount(), 0f, 0f)
    }

    /**
     * Stops cymbal animation.
     */
    fun cancel() {
        animTime = -1.0
    }
}
