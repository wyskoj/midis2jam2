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
import org.wysko.midis2jam2.util.Utils
import kotlin.math.max
import kotlin.math.min

/** Defines angles for cameras. */
enum class Camera(locX: Float, locY: Float, locZ: Float, rotX: Float, rotY: Float, rotZ: Float) {

    /** Camera 1A. */
    CAMERA_1A(-2f, 92f, 134f, Utils.rad(18.44f), Utils.rad(180f), 0f),

    /** Camera 1B. */
    CAMERA_1B(60f, 92f, 124f, Utils.rad(18.5), Utils.rad(204.4), 0f),

    /** Camera 1C. */
    CAMERA_1C(-59.5f, 90.8f, 94.4f, Utils.rad(23.9), Utils.rad(153.6), 0f),

    /** Camera 2A. */
    CAMERA_2A(0f, 71.8f, 44.5f, Utils.rad(15.7), Utils.rad(224.9), 0f),

    /** Camera 2B. */
    CAMERA_2B(-35f, 76.4f, 33.6f, Utils.rad(55.8), Utils.rad(198.5), 0f),

    /** Camera 3A. */
    CAMERA_3A(-0.2f, 61.6f, 38.6f, Utils.rad(15.5), Utils.rad(180f), 0f),

    /** Camera 3B. */
    CAMERA_3B(-19.6f, 78.7f, 3.8f, Utils.rad(27.7), Utils.rad(163.8), 0f),

    /** Camera 4A. */
    CAMERA_4A(0.2f, 81.1f, 32.2f, Utils.rad(21f), Utils.rad(131.8), Utils.rad(-0.5)),

    /** Camera 4B. */
    CAMERA_4B(35f, 25.4f, -19f, Utils.rad(-50f), Utils.rad(119f), Utils.rad(-2.5)),

    /** Camera 5. */
    CAMERA_5(5f, 432f, 24f, Utils.rad(82.875f), Utils.rad(180f), 0f),

    /** Camera 6. */
    CAMERA_6(17f, 30.5f, 42.9f, Utils.rad(-6.7), Utils.rad(144.3), 0f);

    /** The location of the camera. */
    val location: Vector3f = Vector3f(locX, locY, locZ)

    /** The rotation of the camera. */
    val rotation: Quaternion = Quaternion().fromAngles(rotX, rotY, rotZ)

    companion object {
        /** Checks the camera's position and ensures it stays within a certain bounding box. */
        @JvmStatic
        fun preventCameraFromLeaving(camera: com.jme3.renderer.Camera) {
            val location = camera.location
            camera.location = Vector3f(
                if (location.x > 0) min(location.x, 400f) else max(location.x, -400f),
                if (location.y > 0) min(location.y, 432f) else max(location.y, -432f),
                if (location.z > 0) min(location.z, 400f) else max(location.z, -400f)
            )
        }
    }

}