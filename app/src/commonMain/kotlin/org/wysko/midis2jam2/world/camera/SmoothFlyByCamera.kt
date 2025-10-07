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

import com.jme3.input.FlyByCamera
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.CameraController
import org.wysko.midis2jam2.world.VariableSpeed
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val DEFAULT_MOVE_SPEED = 100f

/**
 * The SmoothFlyByCamera class is used to smoothly move and rotate the camera towards a target location and rotation.
 *
 * @property context The Midis2jam2 instance.
 * @property onAction Callback function to be called when an action is performed.
 * @property actLikeNormalFlyByCamera Whether the camera should act like a normal FlyByCamera.
 * @property isEnabled Whether the camera is enabled.
 * @property speed The speed at which the camera moves.
 */
class SmoothFlyByCamera(
    private val context: Midis2jam2,
    private val defaultFov: Float,
    private val onAction: () -> Unit,
) : CameraController, VariableSpeed {
    /**
     * Whether the camera should act like a normal [FlyByCamera].
     */
    var actLikeNormalFlyByCamera: Boolean = false

    /**
     * Whether the camera is enabled.
     */
    override var isEnabled: Boolean = true
        set(value) {
            if (value && !field) {
                dummyCamera.location.set(context.app.camera.location)
                dummyCamera.rotation.set(context.app.camera.rotation)
                dummyCamera.fov = context.app.camera.fov
            }

            field = value
        }

    override fun moveToCameraAngle(cameraAngle: CameraAngle) {
        setTargetTransform(cameraAngle.location, cameraAngle.rotation)
    }

    /**
     * The speed at which the camera moves.
     */
    override var speed: Float = DEFAULT_MOVE_SPEED
        set(value) {
            dummyFlyByCamera.moveSpeed = value
            field = value
        }

    private val dummyCamera = Camera(context.app.camera.width, context.app.camera.height).apply {
        isParallelProjection = false
        fov = defaultFov
    }

    private val dummyFlyByCamera: FlyByCamera = FlyByCameraListenable(dummyCamera) {
        onAction()
    }.apply {
        registerWithInput(context.app.inputManager)
        isDragToRotate =
            !context.configs.find<AppSettingsConfiguration>().appSettings.controlsSettings.isLockCursor
        moveSpeed = DEFAULT_MOVE_SPEED
        zoomSpeed = -10f
    }

    init {
        setTargetTransform(
            Vector3f(-2f, 92f, 134f),
            Quaternion().fromAngles(Utils.rad(18.44f), Utils.rad(180f), 0f)
        )
    }

    /**
     * Updates the camera's location and rotation to slowly move towards the dummy camera's location and rotation.
     */
    override fun tick(delta: Duration) {
        if (!isEnabled) return

        with(context.app.camera) {
            if (actLikeNormalFlyByCamera) {
                location = dummyCamera.location
                rotation = dummyCamera.rotation
                fov = dummyCamera.fov.coerceAtLeast(1f)
            } else {
                location.interpolateLocal(
                    dummyCamera.location,
                    (delta.toDouble(SECONDS) * 3.0f).toFloat()
                )
                rotation.run {
                    slerp(dummyCamera.rotation, (delta.toDouble(SECONDS) * 3.0f).toFloat())
                    normalizeLocal()
                }
                fov = fov.interpolate(dummyCamera.fov, (delta.toDouble(SECONDS) * 3.0f).toFloat())
                    .coerceAtLeast(1f)
            }
        }
    }

    /** Sets the target location and rotation of the camera. */
    fun setTargetTransform(location: Vector3f, rotation: Quaternion) {
        dummyCamera.location.set(location)
        dummyCamera.rotation.set(rotation)
    }
}

/**
 * Interpolates between this and [other] by [speed].
 *
 * @param other the other value to interpolate to
 * @param speed the speed at which to interpolate
 * @return the interpolated value
 */
fun Float.interpolate(other: Float, speed: Float): Float {
    return this + (other - this) * speed
}
