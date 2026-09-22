/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.testing

import com.jme3.input.event.InputEvent
import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera

/**
 * Presses keys and moves sticks at a running performance.
 *
 * Input reaches the engine the same way a real key press does - as a raw event handed to the
 * input manager, which turns it into the app's own actions on the next frame. Tests therefore
 * exercise the real bindings, listeners and camera plugins rather than calling handlers by
 * hand, which is how a binding that stopped being registered would go unnoticed.
 */
class InputHarness(private val performance: HeadlessPerformance) {

    private val inputManager get() = performance.app.inputManager

    /** Holds [key] down. Nothing else changes until it is released. */
    fun press(key: Int) {
        deliver(KeyInputEvent(key, characterFor(key), true, false))
    }

    /** Releases [key]. */
    fun release(key: Int) {
        deliver(KeyInputEvent(key, characterFor(key), false, false))
    }

    /**
     * Hands a raw key event to the input manager and lets it be acted on.
     *
     * The engine only accepts raw input while it is polling the real devices, and a headless
     * context has none to poll. Opening that window deliberately is what lets a test press a
     * key; everything downstream - the bindings, the listeners, the camera plugins - then runs
     * exactly as it does for a real key press.
     */
    private fun deliver(event: InputEvent) {
        performance.onEngineThread {
            val permitted = eventsPermittedField()
            permitted.setBoolean(inputManager, true)
            try {
                when (event) {
                    is KeyInputEvent -> inputManager.onKeyEvent(event)
                    is MouseButtonEvent -> inputManager.onMouseButtonEvent(event)
                    is MouseMotionEvent -> inputManager.onMouseMotionEvent(event)
                    else -> error("Unsupported input event: $event")
                }
            } finally {
                permitted.setBoolean(inputManager, false)
            }
        }
        frames(1)
    }

    /** Presses and releases [key], as a user would tapping it once. */
    fun tap(key: Int) {
        press(key)
        release(key)
    }

    /** Presses a mouse button at [x], [y]. Button 0 is the left one. */
    fun pressMouse(button: Int = 0, x: Int = 400, y: Int = 300) {
        deliver(MouseButtonEvent(button, true, x, y))
    }

    /** Releases a mouse button at [x], [y]. */
    fun releaseMouse(button: Int = 0, x: Int = 400, y: Int = 300) {
        deliver(MouseButtonEvent(button, false, x, y))
    }

    /** Moves the pointer by [dx], [dy] from [fromX], [fromY]. */
    fun moveMouse(dx: Int, dy: Int, fromX: Int = 400, fromY: Int = 300) {
        deliver(MouseMotionEvent(fromX + dx, fromY + dy, dx, dy, 0, 0))
    }

    /** Turns the scroll wheel by [clicks]; positive scrolls one way, negative the other. */
    fun scroll(clicks: Int, x: Int = 400, y: Int = 300) {
        deliver(MouseMotionEvent(x, y, 0, 0, clicks * WHEEL_UNITS_PER_CLICK, clicks))
    }

    /** Drags the pointer with the left button held, as rotating the camera does. */
    fun dragMouse(dx: Int, dy: Int) {
        pressMouse()
        moveMouse(dx, dy)
        releaseMouse(x = 400 + dx, y = 300 + dy)
    }

    /**
     * Lets [count] engine frames pass.
     *
     * Each queued call is drained once per frame, so waiting for one to complete is waiting
     * for a frame.
     */
    fun frames(count: Int = 1) {
        repeat(count) { performance.onEngineThread { } }
    }

    /** A snapshot of where the camera is and which way it faces. */
    fun cameraPose(): CameraPose = performance.onEngineThread {
        val camera: Camera = performance.app.camera
        CameraPose(camera.location.clone(), camera.direction.clone(), camera.fov)
    }

    /** Where the camera is and which way it faces, at one moment. */
    data class CameraPose(val location: Vector3f, val direction: Vector3f, val fieldOfView: Float) {

        /** How far the camera moved between this pose and [other]. */
        fun distanceTo(other: CameraPose): Float = location.distance(other.location)

        /** How far the camera turned between this pose and [other], as a rough angle. */
        fun angleTo(other: CameraPose): Float = direction.angleBetween(other.direction)
    }

    private companion object {

        /** jMonkeyEngine counts a wheel click as this many units of travel. */
        const val WHEEL_UNITS_PER_CLICK = 120

        /** The guard the engine uses to reject input outside its own polling window. */
        private var cachedField: java.lang.reflect.Field? = null

        fun eventsPermittedField(): java.lang.reflect.Field =
            cachedField ?: com.jme3.input.InputManager::class.java
                .getDeclaredField("eventsPermitted")
                .apply { isAccessible = true }
                .also { cachedField = it }

        /**
         * The character a key produces.
         *
         * Only the printable movement keys need one; the bindings match on key code, and the
         * character is carried along for completeness.
         */
        fun characterFor(key: Int): Char = KEY_CHARACTERS[key] ?: ' '

        val KEY_CHARACTERS: Map<Int, Char> = mapOf(
            com.jme3.input.KeyInput.KEY_W to 'w',
            com.jme3.input.KeyInput.KEY_A to 'a',
            com.jme3.input.KeyInput.KEY_S to 's',
            com.jme3.input.KeyInput.KEY_D to 'd',
            com.jme3.input.KeyInput.KEY_Q to 'q',
            com.jme3.input.KeyInput.KEY_Z to 'z',
            com.jme3.input.KeyInput.KEY_SPACE to ' ',
            com.jme3.input.KeyInput.KEY_GRAVE to '`',
        )
    }
}
