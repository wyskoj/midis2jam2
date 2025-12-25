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
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_CAMERA_PLUGIN_FREE
import org.wysko.midis2jam2.manager.PreferencesManager
import org.wysko.midis2jam2.util.state

private const val DEFAULT_MOVE_SPEED = 100f
private const val DEFAULT_ZOOM_SPEED = -10f
private const val INTERPOLATION_SPEED = 3.0f
private const val NUM_CATEGORIES = 6

class FreeCameraPlugin(val onCameraInput: () -> Unit = {}) : CameraPlugin(), ActionListener {
    private val cameraAngleCategories = CameraAngleCategory.categories
    private var category = 1
    private var index = 0
    var movementType: MovementType = MovementType.Normal

    private lateinit var dummyCamera: Camera
    private lateinit var dummyFlyByCamera: FlyByCamera

    override fun initialize(app: Application?) {
        (app as SimpleApplication).flyByCamera.unregisterInput()
        dummyCamera = Camera(app.camera.width, app.camera.height).apply {
            isParallelProjection = false
        }
        dummyFlyByCamera = ExtendedJoystickFlyByCamera(dummyCamera, onCameraInput).apply {
            registerWithInput(app.inputManager)
            isDragToRotate =
                app.state<PreferencesManager>()?.getAppSettings()?.controlsSettings?.isLockCursor?.not() ?: true
            moveSpeed = DEFAULT_MOVE_SPEED
            zoomSpeed = DEFAULT_ZOOM_SPEED
        }
        applyCameraAngle()
        snapCamera()
        app.inputManager.addListener(this, *cameraAngleActions, ACTION_CAMERA_PLUGIN_FREE)
    }

    override fun onEnable() {
        dummyCamera.location = application.camera.location
        dummyCamera.rotation = application.camera.rotation
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
                    fov = fov.interpolate(dummyCamera.fov, tpf * INTERPOLATION_SPEED)
                }
            }
        }
    }

    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
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

    override fun cleanup(app: Application?): Unit = Unit

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