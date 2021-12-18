/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.world

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.world.Camera.*
import kotlin.math.pow

/** The speed at which to transition from one camera angle to another. */
private const val MOVE_SPEED = 0.25f

/** The amount of time to wait before transitioning to the next camera angle. */
private const val WAIT_TIME = 4f

/**
 * The auto-cam controller is responsible for controlling the automatic movement of the camera. It picks camera angles
 * randomly and moves the camera to them.
 */
class AutoCamController(private val context: Midis2jam2) {

    /** When true, the auto-cam controller is enabled. */
    var enabled: Boolean = true

    /** The amount of time that has passed since the last camera angle change. */
    private var waiting = 0f

    /** True if the camera is currently moving to a new angle, false otherwise. */
    private var moving = false

    /** A list of previously used camera angles. */
    private val angles = mutableListOf<Camera>().apply {
        add(CAMERA_1A)
    }

    /** The current amount of transition, from 0 to 1. */
    private var x = 0f

    /** The location at which the camera started at in this transition. */
    private var startLocation: Vector3f = CAMERA_1A.location.clone()

    /** The rotation at which the camera started at in this transition. */
    private var startRotation: Quaternion = CAMERA_1A.rotation.clone()

    /**
     * Selects a random camera angle. It avoids picking the same camera twice in a row, and picking a camera for which
     * no instruments are visible in that angle. This rule applies to:
     * * [Camera.CAMERA_2A], [Camera.CAMERA_2B] — [Keyboard]
     * * [Camera.CAMERA_3A], [Camera.CAMERA_3B] — [Percussion]
     * * [Camera.CAMERA_4A], [Camera.CAMERA_4B] — [BassGuitar]
     * * [Camera.CAMERA_5] — Any
     * * [Camera.CAMERA_6] — [Guitar]
     */
    private fun randomCamera(): Camera = values().filter { cam ->
        !angles.takeLast(2).contains(cam) && when (cam) {
            CAMERA_2A, CAMERA_2B -> {
                context.instruments.filterIsInstance<Keyboard>().any { it.isVisible }
            }
            CAMERA_3A, CAMERA_3B -> {
                context.instruments.filterIsInstance<Percussion>().any { it.isVisible }
            }
            CAMERA_4A, CAMERA_4B -> {
                context.instruments.filterIsInstance<BassGuitar>().any { it.isVisible }
            }
            CAMERA_5 -> {
                context.instruments.filterNotNull().count { it.isVisible } > 3
                        && Math.random() < 0.2
            }
            CAMERA_6 -> {
                context.instruments.filterIsInstance<Guitar>().any { it.isVisible }
            }
            else -> true
        }
    }.random()

    /**
     * Sometimes, when a camera focuses on an instrument, it may become invisible and the camera should move to a
     * different angle. This method checks if that is the case and moves the camera if so.
     */
    private fun leaveIfGone() {
        when (angles.last()) {
            CAMERA_2A, CAMERA_2B -> {
                if (context.instruments.filterIsInstance<Keyboard>().none { it.isVisible }) {
                    trigger()
                }
            }
            CAMERA_3A, CAMERA_3B -> {
                if (context.instruments.filterIsInstance<Percussion>().none { it.isVisible }) {
                    trigger()
                }
            }
            CAMERA_4A, CAMERA_4B -> {
                if (context.instruments.filterIsInstance<BassGuitar>().none { it.isVisible }) {
                    trigger()
                }
            }
            CAMERA_5 -> {
                if (context.instruments.filterNotNull().none { it.isVisible }) {
                    trigger()
                }
            }
            CAMERA_6 -> {
                if (context.instruments.filterIsInstance<Guitar>().none { it.isVisible }) {
                    trigger()
                }
            }
            else -> {}
        }
    }

    /** Performs a tick of the auto-cam controller. */
    fun tick(time: Double, delta: Float) {
        if (!enabled) return

        if (!moving && time > 0) {
            waiting += delta
            leaveIfGone()
            startLocation = context.app.camera.location.clone()
            startRotation = context.app.camera.rotation.clone()
        }

        if (waiting >= WAIT_TIME) {
            waiting = 0f
            angles.add(randomCamera())
            moving = true
        }

        if (moving) {
            x += delta * MOVE_SPEED
            cam.location = Vector3f().interpolateLocal(startLocation, angles.last().location, x.smooth())
            cam.rotation = quaternionInterp(startRotation, angles.last().rotation, x.smooth())
            if (x > 1f) {
                x = 0f
                moving = false
            }
        }
    }

    /** Moves the camera to a new position, if it is not curently moving. */
    fun trigger() {
        enabled = true
        if (!moving) {
            waiting = WAIT_TIME
        }
    }

    /** The camera. */
    val cam: com.jme3.renderer.Camera
        get() = context.app.camera
}


/**
 * Performs an interpolation between two quaternions.
 */
fun quaternionInterp(start: Quaternion, end: Quaternion, x: Float): Quaternion = Quaternion(
    start.x + (end.x - start.x) * x,
    start.y + (end.y - start.y) * x,
    start.z + (end.z - start.z) * x,
    start.w + (end.w - start.w) * x
).apply { this.normalizeLocal() }


/**
 * Applies cubic-ease-in-out interpolation to a value.
 */
fun Float.smooth(): Float = if (this < 0.5) 4 * this.pow(3) else 1 - (-2 * this + 2).pow(3) / 2