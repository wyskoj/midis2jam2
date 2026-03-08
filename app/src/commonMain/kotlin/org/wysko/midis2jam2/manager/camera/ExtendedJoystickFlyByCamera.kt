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

package org.wysko.midis2jam2.manager.camera

import com.jme3.input.*
import com.jme3.input.controls.KeyTrigger
import com.jme3.input.controls.MouseAxisTrigger
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera

private const val FLYCAM_LEFT_ANALOG = "FLYCAM_Left_Analog"
private const val FLYCAM_RIGHT_ANALOG = "FLYCAM_Right_Analog"
private const val FLYCAM_UP_ANALOG = "FLYCAM_Up_Analog"
private const val FLYCAM_DOWN_ANALOG = "FLYCAM_Down_Analog"
private const val FLYCAM_RISE_ANALOG = "FLYCAM_Raise_Analog"
private const val FLYCAM_LOWER_ANALOG = "FLYCAM_Lower_Analog"

private const val JOYSTICK_SENSITIVITY = 0.5f

private val MAPPINGS: Array<String> = arrayOf(
    CameraInput.FLYCAM_LEFT,
    CameraInput.FLYCAM_RIGHT,
    CameraInput.FLYCAM_UP,
    CameraInput.FLYCAM_DOWN,
    CameraInput.FLYCAM_STRAFELEFT,
    CameraInput.FLYCAM_STRAFERIGHT,
    CameraInput.FLYCAM_FORWARD,
    CameraInput.FLYCAM_BACKWARD,
    CameraInput.FLYCAM_ZOOMIN,
    CameraInput.FLYCAM_ZOOMOUT,
    CameraInput.FLYCAM_ROTATEDRAG,
    CameraInput.FLYCAM_RISE,
    CameraInput.FLYCAM_LOWER,
    CameraInput.FLYCAM_INVERTY,
    FLYCAM_LEFT_ANALOG,
    FLYCAM_RIGHT_ANALOG,
    FLYCAM_UP_ANALOG,
    FLYCAM_DOWN_ANALOG,
    FLYCAM_RISE_ANALOG,
    FLYCAM_LOWER_ANALOG,
)

private val INTERRUPTIBLE_ACTIONS = arrayOf(
    CameraInput.FLYCAM_ROTATEDRAG,
    CameraInput.FLYCAM_STRAFELEFT,
    CameraInput.FLYCAM_STRAFERIGHT,
    CameraInput.FLYCAM_FORWARD,
    CameraInput.FLYCAM_BACKWARD,
    CameraInput.FLYCAM_RISE,
    CameraInput.FLYCAM_LOWER,
    FLYCAM_LEFT_ANALOG,
    FLYCAM_RIGHT_ANALOG,
    FLYCAM_UP_ANALOG,
    FLYCAM_DOWN_ANALOG,
    FLYCAM_RISE_ANALOG,
    FLYCAM_LOWER_ANALOG,
)

class ExtendedJoystickFlyByCamera(camera: Camera, val onInput: () -> Unit = {}) : FlyByCamera(camera) {
    override fun registerWithInput(inputManager: InputManager) {
        this.inputManager = inputManager


        // both mouse and button - rotation of cam
        inputManager.addMapping(
            CameraInput.FLYCAM_LEFT, MouseAxisTrigger(MouseInput.AXIS_X, true),
            KeyTrigger(KeyInput.KEY_LEFT)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_RIGHT, MouseAxisTrigger(MouseInput.AXIS_X, false),
            KeyTrigger(KeyInput.KEY_RIGHT)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_UP, MouseAxisTrigger(MouseInput.AXIS_Y, false),
            KeyTrigger(KeyInput.KEY_UP)
        )

        inputManager.addMapping(
            CameraInput.FLYCAM_DOWN, MouseAxisTrigger(MouseInput.AXIS_Y, true),
            KeyTrigger(KeyInput.KEY_DOWN)
        )


        // mouse only - zoom in/out with wheel, and rotate drag
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMIN, MouseAxisTrigger(MouseInput.AXIS_WHEEL, false))
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMOUT, MouseAxisTrigger(MouseInput.AXIS_WHEEL, true))
        inputManager.addMapping(CameraInput.FLYCAM_ROTATEDRAG, MouseButtonTrigger(MouseInput.BUTTON_LEFT))


        // keyboard only WASD for movement and WZ for rise/lower height
        inputManager.addMapping(CameraInput.FLYCAM_STRAFELEFT, KeyTrigger(KeyInput.KEY_A))
        inputManager.addMapping(CameraInput.FLYCAM_STRAFERIGHT, KeyTrigger(KeyInput.KEY_D))
        inputManager.addMapping(CameraInput.FLYCAM_FORWARD, KeyTrigger(KeyInput.KEY_W))
        inputManager.addMapping(CameraInput.FLYCAM_BACKWARD, KeyTrigger(KeyInput.KEY_S))
        inputManager.addMapping(CameraInput.FLYCAM_RISE, KeyTrigger(KeyInput.KEY_Q))
        inputManager.addMapping(CameraInput.FLYCAM_LOWER, KeyTrigger(KeyInput.KEY_Z))

        inputManager.addListener(this, *MAPPINGS)
        inputManager.isCursorVisible = dragToRotate || !isEnabled

        inputManager.joysticks?.firstOrNull()?.let { joystick ->
            joystick.axes[0].assignAxis(CameraInput.FLYCAM_STRAFERIGHT, CameraInput.FLYCAM_STRAFELEFT)
            joystick.axes[1].assignAxis(CameraInput.FLYCAM_BACKWARD, CameraInput.FLYCAM_FORWARD)
            joystick.axes[3].assignAxis(FLYCAM_RIGHT_ANALOG, FLYCAM_LEFT_ANALOG)
            joystick.axes[4].assignAxis(FLYCAM_DOWN_ANALOG, FLYCAM_UP_ANALOG)
            joystick.axes[5].assignAxis(FLYCAM_RISE_ANALOG, FLYCAM_LOWER_ANALOG)
            joystick.axes[2].assignAxis(FLYCAM_LOWER_ANALOG, FLYCAM_RISE_ANALOG)
        }
    }

    override fun onAnalog(name: String?, value: Float, tpf: Float) {
        if (!enabled) return
        if (name in INTERRUPTIBLE_ACTIONS) onInput()

        when (name) {
            CameraInput.FLYCAM_LEFT -> rotateCamera(value, initialUpVec)
            CameraInput.FLYCAM_RIGHT -> rotateCamera(-value, initialUpVec)
            CameraInput.FLYCAM_UP -> rotateCamera(-value * (if (invertY) -1 else 1), cam.left)
            CameraInput.FLYCAM_DOWN -> rotateCamera(value * (if (invertY) -1 else 1), cam.left)
            CameraInput.FLYCAM_FORWARD -> moveCamera(value, false)
            CameraInput.FLYCAM_BACKWARD -> moveCamera(-value, false)
            CameraInput.FLYCAM_STRAFELEFT -> moveCamera(value, true)
            CameraInput.FLYCAM_STRAFERIGHT -> moveCamera(-value, true)
            CameraInput.FLYCAM_RISE -> riseCamera(value)
            CameraInput.FLYCAM_LOWER -> riseCamera(-value)
            CameraInput.FLYCAM_ZOOMIN -> zoomCamera(value)
            CameraInput.FLYCAM_ZOOMOUT -> zoomCamera(-value)
            FLYCAM_LEFT_ANALOG -> joyRotateCamera(value, initialUpVec)
            FLYCAM_RIGHT_ANALOG -> joyRotateCamera(-value, initialUpVec)
            FLYCAM_UP_ANALOG -> joyRotateCamera(-value * (if (invertY) -1 else 1), cam.left)
            FLYCAM_DOWN_ANALOG -> joyRotateCamera(value * (if (invertY) -1 else 1), cam.left)
            FLYCAM_RISE_ANALOG -> joyRiseCamera(value)
            FLYCAM_LOWER_ANALOG -> joyRiseCamera(-value)
        }
    }

    private fun joyRiseCamera(value: Float) {
        riseCamera(value * JOYSTICK_SENSITIVITY)
    }

    private fun joyRotateCamera(value: Float, axis: Vector3f) {
        val oldCanRotate = canRotate
        canRotate = true
        rotateCamera(value * JOYSTICK_SENSITIVITY, axis)
        canRotate = oldCanRotate
    }

}