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

package org.wysko.midis2jam2

import com.jme3.font.BitmapText
import com.jme3.math.FastMath
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.plus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.CameraController
import org.wysko.midis2jam2.world.camera.CameraAngle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val FAKE_ORIGIN_DEFAULT_DISTANCE = 100f

private val orbitCameraState: Map<CameraAngle, OrbitingCameraState> = mapOf(
    CameraAngle.CAMERA_1A to OrbitingCameraState(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 90f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    CameraAngle.CAMERA_1B to OrbitingCameraState(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 60f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    CameraAngle.CAMERA_1C to OrbitingCameraState(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 120f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    CameraAngle.CAMERA_2A to OrbitingCameraState(
        orbitOrigin = v3(-50, 32, -6),
        azimuthAngle = 45f,
        verticalAngle = 25f,
        orbitDistance = 80f
    ),
    CameraAngle.CAMERA_2B to OrbitingCameraState(
        orbitOrigin = v3(-53, 30, -3),
        azimuthAngle = 70f,
        verticalAngle = 45f,
        orbitDistance = FAKE_ORIGIN_DEFAULT_DISTANCE
    ),
    CameraAngle.CAMERA_3A to OrbitingCameraState(
        orbitOrigin = v3(0, 30, -70),
        azimuthAngle = 90f,
        verticalAngle = 15f,
        orbitDistance = 100f
    ),
    CameraAngle.CAMERA_3B to OrbitingCameraState(
        orbitOrigin = v3(0, 30, -70),
        azimuthAngle = 115f,
        verticalAngle = 30f,
        orbitDistance = 80f
    ),
    CameraAngle.CAMERA_4A to OrbitingCameraState(
        orbitOrigin = v3(50, 56, -17),
        azimuthAngle = 132f,
        verticalAngle = 12f,
        orbitDistance = 80f
    ),
    CameraAngle.CAMERA_4B to OrbitingCameraState(
        orbitOrigin = v3(45, 42, -24),
        azimuthAngle = 142f,
        verticalAngle = -55f,
        orbitDistance = 20f
    ),
    CameraAngle.CAMERA_5 to OrbitingCameraState(
        orbitOrigin = v3(0, 40, 0),
        azimuthAngle = 90f,
        verticalAngle = 85f,
        orbitDistance = 400f
    ),
    CameraAngle.CAMERA_6A to OrbitingCameraState(
        orbitOrigin = v3(47, 35, 7),
        azimuthAngle = 127f,
        verticalAngle = 5f,
        orbitDistance = FAKE_ORIGIN_DEFAULT_DISTANCE
    ),
    // 6B requires an upside-down view, which is not possible with this camera's internal math.
    // Thus, I'm omitting it.
)

class AndroidOrbitingCamera(private val context: Midis2jam2) : CameraController {
    private var orbitOrigin = v3(-2, 35, -30)
    private var orbitOriginInterpolated = orbitOrigin.clone()
    private var azimuthAngle = 90f
    private var verticalAngle = 18.44f
    private var orbitDistance = 150.0f
    override var isEnabled: Boolean = true

    override fun tick(delta: Duration) {
        if (!isEnabled) return

        val fDelta = delta.toDouble(DurationUnit.SECONDS).toFloat()
        context.app.camera.location.interpolateLocal(calculateCameraPosition(), fDelta * 10f)
        orbitOriginInterpolated.interpolateLocal(orbitOrigin, fDelta * 10f)
        context.app.camera.lookAt(orbitOriginInterpolated, Vector3f.UNIT_Y)
    }

    fun zoom(delta: Float) {
        isEnabled = true
        // Make zoom more sensitive when closer (smaller orbitDistance)
        val zoomSensitivity = (orbitDistance / 400f).coerceIn(0.1f, 1.0f)
        orbitDistance += delta * 200f * zoomSensitivity
        orbitDistance = orbitDistance.coerceIn(5f, 400f)
    }

    fun orbit(deltaX: Float, deltaY: Float) {
        isEnabled = true
        azimuthAngle += deltaX * 0.1f
        verticalAngle += deltaY * 0.1f
    }

    fun pan(x: Float, y: Float) {
        isEnabled = true
        val camRot = context.app.camera.rotation

        val right = camRot.getRotationColumn(0)
        val up = camRot.getRotationColumn(1)

        val horizontalMove = right.mult(x * 0.1f)
        val verticalMove = up.mult(y * 0.1f)

        val totalMove = horizontalMove + verticalMove

        orbitOrigin.addLocal(totalMove)
    }

    override fun moveToCameraAngle(cameraAngle: CameraAngle) {
        isEnabled = true
        orbitCameraState[cameraAngle]?.let {
            applyOrbitingCameraState(it)
        }
    }

    private fun calculateCameraPosition(): Vector3f {
        val x = orbitDistance * cos(rad(azimuthAngle)) * cos(rad(verticalAngle))
        val y = orbitDistance * sin(rad(verticalAngle))
        val z = orbitDistance * sin(rad(azimuthAngle)) * cos(rad(verticalAngle))
        return orbitOrigin + v3(x, y, z)
    }

    private fun applyOrbitingCameraState(state: OrbitingCameraState) {
        orbitOrigin.set(state.orbitOrigin)
        azimuthAngle = state.azimuthAngle
        verticalAngle = state.verticalAngle
        orbitDistance = state.orbitDistance
    }

    fun applyFakeOrigin() {
        val forward = context.app.camera.rotation.getRotationColumn(2).normalizeLocal()
        val origin = context.app.camera.location.clone()
            .addLocal(forward.clone().multLocal(FAKE_ORIGIN_DEFAULT_DISTANCE))

        val horizontalComponent = Vector2f(forward.x, forward.z).normalizeLocal()

        val azimuth = azimuth(horizontalComponent.x, horizontalComponent.y)
        val vertical = FastMath.asin(-forward.y)

        this.orbitOrigin.set(origin)
        this.orbitOriginInterpolated.set(origin)
        this.azimuthAngle = azimuth
        this.verticalAngle = vertical * 180f / FastMath.PI
        this.orbitDistance = FAKE_ORIGIN_DEFAULT_DISTANCE
    }
}

private data class OrbitingCameraState(
    val orbitOrigin: Vector3f,
    val azimuthAngle: Float,
    val verticalAngle: Float,
    val orbitDistance: Float
)

fun azimuth(x: Float, z: Float): Float =
    ((180.0 + Math.toDegrees(FastMath.atan2(z, x).toDouble())) % 360.0).toFloat()