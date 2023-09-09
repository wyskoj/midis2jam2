/*
 * Copyright (C) 2023 Jacob Wysko
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RADIUS = 134f
private const val Y_BASELINE = 92f
private const val MAX_ANGLE = 0.5f
private const val ROTATION_SPEED_FACTOR = 0.04f
private const val DAMPENING = 0.6f
private const val ROTATION_DAMPENING_FACTOR = 7f
private const val SECONDS_LEFT_UNTIL_BRAKES = 20f

/** Slides the camera back and forth, like how MIDIJam did when you pressed the 9 key. */
class SlideCameraController(
    private val context: Midis2jam2
) {
    /** True if the camera is currently sliding. */
    var isEnabled: Boolean = false
    
    private var goingRight = true
    private var t = 0f
    private var brakesApplied = false
    private var angleWhenBrakesApplied = 0f

    /** Called every frame. */
    fun tick(tpf: Float, time: Double) {
        if (!isEnabled) return


        if (goingRight) {
            t += tpf * ROTATION_SPEED_FACTOR
        } else {
            t -= tpf * ROTATION_SPEED_FACTOR
        }

        if (t > MAX_ANGLE) goingRight = false else if (t < -MAX_ANGLE) goingRight = true

        if (context.file.length - time < SECONDS_LEFT_UNTIL_BRAKES) {
            brakesApplied = true
            angleWhenBrakesApplied = t
        }

        val x =
            (if (brakesApplied) 1f - ((context.file.length - 2) - time) / SECONDS_LEFT_UNTIL_BRAKES else 0f).toFloat()
                .coerceAtMost(1f)

        context.app.camera.location.interpolateLocal(
            desiredLocation(Utils.lerp(t, 0f, x)), (DAMPENING * tpf).also { println(it) }
        )
        context.app.camera.rotation.slerp(
            lookAtRotation(context.app.camera.location.clone(), Vector3f(-2f, 47.320206f, 0f)),
            DAMPENING * 5 * tpf
        )
        context.app.camera.rotation.normalizeLocal()
    }

    private fun desiredLocation(t: Float): Vector3f {
        return Vector3f(
            RADIUS * sin(tAdjusted(t)),
            Y_BASELINE,
            RADIUS * cos(tAdjusted(t))
        )
    }

    private fun tAdjusted(t: Float): Float {
        return sin(t * PI / 2f).toFloat()
    }
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