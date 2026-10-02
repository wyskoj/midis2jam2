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

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.input.FlyByCamera
import com.jme3.input.controls.ActionListener
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_CAMERA_MODIFIER_FAST
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_CAMERA_MODIFIER_SLOW
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_CAMERA_PLUGIN_FREE
import org.wysko.midis2jam2.manager.performanceConfig

private const val DEFAULT_MOVE_SPEED = 100f
private const val SLOW_MOVE_SPEED = 10f
private const val FAST_MOVE_SPEED = 200f
private const val DEFAULT_ZOOM_SPEED = -10f
private const val INTERPOLATION_SPEED = 3.0f
private const val NUM_CATEGORIES = 6
/** The field of view, in degrees, the free camera is kept within. */
internal val FOV_VALID_RANGE = 5f..150f

class FreeCameraPlugin(val onCameraInput: () -> Unit = {}) : CameraPlugin(), ActionListener {
    private val cameraAngleCategories = CameraAngleCategory.categories
    private var category = 1
    private var index = 0
    var movementType: MovementType = MovementType.Normal

    private lateinit var dummyCamera: Camera
    private lateinit var dummyFlyByCamera: FlyByCamera
    private var registeredActions: Array<String> = emptyArray()

    /** Which speed-modifier action is currently held/toggled on, or `null` for normal speed. */
    private var activeSpeedModifier: String? = null

    override fun initialize(app: Application?) {
        (app as SimpleApplication).flyByCamera.unregisterInput()
        dummyCamera = Camera(app.camera.width, app.camera.height).apply {
            isParallelProjection = false
            fov = app.performanceConfig.settings.cameraSettings.defaultFieldOfView
        }
        dummyFlyByCamera = ExtendedJoystickFlyByCamera(
            dummyCamera,
            onCameraInput,
            app.performanceConfig.settings.controlsSettings.isGamepadEnabled,
        ).apply {
            registerWithInput(app.inputManager)
            isDragToRotate =
                !app.performanceConfig.settings.controlsSettings.isLockCursor
            moveSpeed = DEFAULT_MOVE_SPEED
            zoomSpeed = DEFAULT_ZOOM_SPEED
        }
        applyCameraAngle()
        snapCamera()
        registeredActions =
            cameraAngleActions + ACTION_CAMERA_PLUGIN_FREE + ACTION_CAMERA_MODIFIER_SLOW + ACTION_CAMERA_MODIFIER_FAST
        app.inputManager.addListener(this, *registeredActions)
    }

    override fun onEnable() {
        dummyCamera.location = application.camera.location
        dummyCamera.rotation = application.camera.rotation
        activeSpeedModifier = null
        dummyFlyByCamera.moveSpeed = DEFAULT_MOVE_SPEED
    }

    override fun onDisable(): Unit = Unit

    override fun update(tpf: Float) {
        when (movementType) {
            MovementType.Normal -> snapCamera()
            MovementType.Smooth -> {
                application.camera.run {
                    location.interpolateLocal(dummyCamera.location, tpf * INTERPOLATION_SPEED)
                    rotation.run {
                        slerp(dummyCamera.rotation, tpf * INTERPOLATION_SPEED)
                        normalizeLocal()
                    }
                    fov = fov.interpolate(dummyCamera.fov, tpf * INTERPOLATION_SPEED).coerceIn(FOV_VALID_RANGE)
                }
            }
        }
    }

    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        if (name == ACTION_CAMERA_MODIFIER_SLOW || name == ACTION_CAMERA_MODIFIER_FAST) {
            handleSpeedModifier(name, isPressed)
            return
        }

        if (!isPressed) return

        when (name) {
            ACTION_CAMERA_PLUGIN_FREE -> {
                category = 1
                index = 0
            }

            else -> applyCameraCategory(name)
        }

        applyCameraAngle()
    }

    /**
     * Applies [name]'s (a speed-modifier action) press/release to [dummyFlyByCamera]'s move speed.
     *
     * In non-sticky mode, the modifier is active only while the key is held. In sticky mode, it's
     * toggled on release, and stays active until the same key - or the other modifier - is pressed
     * again.
     */
    private fun handleSpeedModifier(name: String, isPressed: Boolean) {
        val isSticky = application.performanceConfig.settings.controlsSettings.isSpeedModifierKeysSticky

        activeSpeedModifier = when (isSticky) {
            true -> {
                if (isPressed) return // Only react on release
                if (activeSpeedModifier == name) null else name
            }

            false -> if (isPressed) name else null
        }

        dummyFlyByCamera.moveSpeed = when (activeSpeedModifier) {
            ACTION_CAMERA_MODIFIER_SLOW -> SLOW_MOVE_SPEED
            ACTION_CAMERA_MODIFIER_FAST -> FAST_MOVE_SPEED
            else -> DEFAULT_MOVE_SPEED
        }
    }

    override fun cleanup(app: Application?) {
        val inputManager = app?.inputManager ?: return
        inputManager.removeListener(this)
        if (registeredActions.isNotEmpty()) {
            registeredActions.forEach { action ->
                if (inputManager.hasMapping(action)) {
                    inputManager.deleteMapping(action)
                }
            }
        }
        dummyFlyByCamera.unregisterInput()
        registeredActions = emptyArray()
    }

    private fun applyCameraCategory(name: String) {
        val targetCategory = name.last().digitToInt()

        when (targetCategory == category) {
            true -> incrementIndex()
            false -> {
                if (cameraAngleCategories.any { it.category == targetCategory }) {
                    category = targetCategory
                    index = 0
                }
            }
        }
    }

    private fun incrementIndex() {
        index++
        cameraAngleCategories.find { it.category == category }?.angles?.lastIndex?.let {
            if (index > it) {
                index = 0
            }
        }
    }

    private fun applyCameraAngle() {
        val angle = cameraAngleCategories.find { it.category == category }?.angles[index]
        dummyCamera.location = angle?.location
        dummyCamera.lookAt(angle?.lookAt, Vector3f.UNIT_Y)
    }

    private fun snapCamera() {
        application.camera.location = dummyCamera.location
        application.camera.rotation = dummyCamera.rotation
        application.camera.fov = dummyCamera.fov
    }

    private fun Float.interpolate(target: Float, tpf: Float): Float {
        return this + (target - this) * tpf
    }

    companion object {
        val cameraAngleActions: Array<String> by lazy {
            Array(NUM_CATEGORIES) { "camera_angle_${it + 1}" }
        }
    }

    enum class MovementType {
        Normal, Smooth
    }
}
