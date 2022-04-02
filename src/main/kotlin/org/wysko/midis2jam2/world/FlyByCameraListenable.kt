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

package org.wysko.midis2jam2.world

import com.jme3.input.FlyByCamera
import com.jme3.renderer.Camera

/**
 * A [FlyByCamera] that calls a function whenever the camera is moved or rotated by the user.
 *
 * @param cam the camera that should be controlled by this
 * @param onUserInput a callback that will run everytime an action occurs
 */
class FlyByCameraListenable(cam: Camera, val onUserInput: (name: String) -> Unit) : FlyByCamera(cam) {

    /** Determines the state of "dragging". That is, is the mouse being held down? */
    private var dragging: Boolean = false
        set(value) {
            if (value) onUserInput("FLYCAM_RotateDrag")
            field = value
        }

    /** Called whenever an action occurs. */
    override fun onAction(name: String?, value: Boolean, tpf: Float) {
        super.onAction(name, value, tpf)
        if (name == "FLYCAM_RotateDrag") dragging = value
        if (value && (name == "FLYCAM_Forward" || name == "FLYCAM_Backward" || name == "FLYCAM_StrafeLeft" || name == "FLYCAM_StrafeRight")) onUserInput(
            name
        )
    }
}