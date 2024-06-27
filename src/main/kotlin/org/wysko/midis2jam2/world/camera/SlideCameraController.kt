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

package org.wysko.midis2jam2.world.camera

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.util.TempVars
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.v3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private const val RADIUS = 134
private const val Y_BASELINE = 92
private const val MAX_ANGLE = 0.5
private const val ROTATION_SPEED_FACTOR = 0.04
private const val DAMPENING = 0.6
private val TIME_LEFT_UNTIL_BRAKES = 20.seconds

/** Slides the camera back and forth, like how MIDIJam did when you pressed the "nine" key. */
context(Midis2jam2)
class SlideCameraController {

    /** `true` if the camera is currently sliding, `false` otherwise. */
    var isEnabled: Boolean = false

    private var goingRight = true
    private var t = 0.0
    private var brakesApplied = false
    private var angleWhenBrakesApplied = 0.0

    /**
     * Called every frame.
     *
     * @param delta The amount of time that has passed since the last frame.
     * @param time The current time in the song.
     */
    fun tick(time: Duration, delta: Duration) {
        if (!isEnabled) return

        with((delta * ROTATION_SPEED_FACTOR).toDouble(SECONDS)) {
            if (goingRight) {
                t += this
            } else {
                t -= this
            }
        }

        if (t > MAX_ANGLE) goingRight = false else if (t < -MAX_ANGLE) goingRight = true

        brakesApplied = if (sequence.duration - time < TIME_LEFT_UNTIL_BRAKES) {
            if (!brakesApplied) {
                angleWhenBrakesApplied = t
            }
            true
        } else {
            false
        }

        val brakesFactor = let {
            if (brakesApplied) {
                1f - ((sequence.duration - 2.seconds) - time) / TIME_LEFT_UNTIL_BRAKES
            } else {
                0
            }
        }.toFloat().coerceAtMost(1f)

        app.camera.location.interpolateLocal(
            desiredLocation(Utils.lerp(t.toFloat(), 0f, brakesFactor)),
            (DAMPENING * delta.toDouble(SECONDS)).toFloat()
        )
        app.camera.rotation.slerp(
            lookAtRotation(app.camera.location.clone(), Vector3f(-2f, 47.320206f, 0f)),
            (DAMPENING * 5 * delta.toDouble(SECONDS)).toFloat()
        )
        app.camera.rotation.normalizeLocal()
    }

    /**
     * Call when the file loops.
     */
    fun onLoop() {
        t = 0.0
        brakesApplied = false
    }

    private fun desiredLocation(t: Float): Vector3f = v3(
        x = RADIUS * sin(tAdjusted(t)),
        y = Y_BASELINE,
        z = RADIUS * cos(tAdjusted(t))
    )

    private fun tAdjusted(t: Float): Float = sin(t * PI / 2f).toFloat()
}

/**
 * Returns a quaternion that looks at [pos] from [loc].
 *
 * @param loc The location to look from.
 * @param pos The location to look at.
 * @return A quaternion that looks at [pos] from [loc].
 */
fun lookAtRotation(loc: Vector3f, pos: Vector3f): Quaternion {
    val vars = TempVars.get()
    val newDirection = vars.vect1
    val newUp = vars.vect2
    val newLeft = vars.vect3
    newDirection.set(pos).subtractLocal(loc).normalizeLocal()
    newUp.set(Vector3f.UNIT_Y).normalizeLocal()
    if (newUp == Vector3f.ZERO) {
        newUp.set(Vector3f.UNIT_Y)
    }

    newLeft.set(newUp).crossLocal(newDirection).normalizeLocal()
    if (newLeft == Vector3f.ZERO) {
        if (newDirection.x != 0f) {
            newLeft[newDirection.y, -newDirection.x] = 0f
        } else {
            newLeft[0f, newDirection.z] = -newDirection.y
        }
    }
    newUp.set(newDirection).crossLocal(newLeft).normalizeLocal()
    vars.release()
    return Quaternion().fromAxes(newLeft, newUp, newDirection).normalizeLocal()
}
