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

package org.wysko.midis2jam2.world.camera

/**
 * Represents the speed of the camera.
 *
 * @property speedValue The speed of the camera.
 */
enum class CameraSpeed(val speedValue: Float) {
    /**
     * The normal speed of the camera.
     */
    Normal(100f),

    /**
     * The slow speed of the camera.
     */
    Slow(10f),

    /**
     * The fast speed of the camera.
     */
    Fast(200f);

    companion object {
        /**
         * Returns the camera speed corresponding to the specified speed.
         *
         * @param speed The speed of the camera
         * @return The camera speed corresponding to the specified speed
         */
        operator fun get(speed: String): CameraSpeed = when (speed.trim().lowercase()) {
            "slow" -> Slow
            "fast" -> Fast
            else -> Normal
        }
    }
}