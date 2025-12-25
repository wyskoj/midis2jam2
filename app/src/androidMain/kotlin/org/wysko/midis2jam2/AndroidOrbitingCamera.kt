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

import com.jme3.app.Application
import com.jme3.math.FastMath
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.manager.camera.CameraPlugin
import org.wysko.midis2jam2.manager.camera.MidiJamAngleEngine
import org.wysko.midis2jam2.manager.camera.MidiJamAngleEngine.Angle
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.plus
import org.wysko.midis2jam2.util.v3
import kotlin.math.cos
import kotlin.math.sin

private const val FAKE_ORIGIN_DEFAULT_DISTANCE = 100f

class AndroidOrbitingCamera : CameraPlugin() {
    private var orbitOrigin = v3(-2, 35, -30)
    private var orbitOriginInterpolated = orbitOrigin.clone()
    private var azimuthAngle = 90f
    private var verticalAngle = 18.44f
    private var orbitDistance = 150.0f

    private val angleEngine = MidiJamAngleEngine(
        prohibitedAngles = setOf(Angle.Angle6B)
    )

    override fun update(tpf: Float) {
        application.camera.location.interpolateLocal(calculateCameraPosition(), tpf * 10f)
        orbitOriginInterpolated.interpolateLocal(orbitOrigin, tpf * 10f)
        application.camera.lookAt(orbitOriginInterpolated, Vector3f.UNIT_Y)
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
        val camRot = application.camera.rotation

        val right = camRot.getRotationColumn(0)
        val up = camRot.getRotationColumn(1)

        val horizontalMove = right.mult(x * 0.1f)
        val verticalMove = up.mult(y * 0.1f)

        val totalMove = horizontalMove + verticalMove

        orbitOrigin.addLocal(totalMove)
    }

    fun applyFakeOrigin() {
        val forward = application.camera.rotation.getRotationColumn(2).normalizeLocal()
        val origin = application.camera.location.clone()
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

    fun applyCameraCategory(targetCategory: Int) {
        angleEngine.triggerCategory(targetCategory)
        angleDefinitions[angleEngine.currentAngle]?.let(::applyOrbitingCameraState)
    }

    private fun calculateCameraPosition(): Vector3f {
        val x = orbitDistance * cos(rad(azimuthAngle)) * cos(rad(verticalAngle))
        val y = orbitDistance * sin(rad(verticalAngle))
        val z = orbitDistance * sin(rad(azimuthAngle)) * cos(rad(verticalAngle))
        return orbitOrigin + v3(x, y, z)
    }

    private fun applyOrbitingCameraState(state: State) {
        orbitOrigin.set(state.orbitOrigin)
        azimuthAngle = state.azimuthAngle
        verticalAngle = state.verticalAngle
        orbitDistance = state.orbitDistance
    }

    private fun azimuth(x: Float, z: Float): Float =
        ((180.0 + Math.toDegrees(FastMath.atan2(z, x).toDouble())) % 360.0).toFloat()


    internal data class State(
        val orbitOrigin: Vector3f,
        val azimuthAngle: Float,
        val verticalAngle: Float,
        val orbitDistance: Float,
    )

    override fun onEnable(): Unit = Unit
    override fun initialize(app: Application?): Unit = Unit
    override fun cleanup(app: Application?): Unit = Unit
    override fun onDisable(): Unit = Unit
}


private val angleDefinitions: Map<Angle, AndroidOrbitingCamera.State> = mapOf(
    Angle.Angle1A to AndroidOrbitingCamera.State(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 90f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    Angle.Angle1B to AndroidOrbitingCamera.State(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 60f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    Angle.Angle1C to AndroidOrbitingCamera.State(
        orbitOrigin = v3(-2, 35, -30),
        azimuthAngle = 120f,
        verticalAngle = 18.44f,
        orbitDistance = 150f
    ),
    Angle.Angle2A to AndroidOrbitingCamera.State(
        orbitOrigin = v3(-50, 32, -6),
        azimuthAngle = 45f,
        verticalAngle = 25f,
        orbitDistance = 80f
    ),
    Angle.Angle2B to AndroidOrbitingCamera.State(
        orbitOrigin = v3(-53, 30, -3),
        azimuthAngle = 70f,
        verticalAngle = 45f,
        orbitDistance = 50f,
    ),
    Angle.Angle3A to AndroidOrbitingCamera.State(
        orbitOrigin = v3(0, 30, -70),
        azimuthAngle = 90f,
        verticalAngle = 15f,
        orbitDistance = 100f
    ),
    Angle.Angle3B to AndroidOrbitingCamera.State(
        orbitOrigin = v3(0, 30, -70),
        azimuthAngle = 115f,
        verticalAngle = 30f,
        orbitDistance = 80f
    ),
    Angle.Angle4A to AndroidOrbitingCamera.State(
        orbitOrigin = v3(50, 56, -17),
        azimuthAngle = 132f,
        verticalAngle = 12f,
        orbitDistance = 80f
    ),
    Angle.Angle4B to AndroidOrbitingCamera.State(
        orbitOrigin = v3(45, 42, -24),
        azimuthAngle = 142f,
        verticalAngle = -55f,
        orbitDistance = 20f
    ),
    Angle.Angle5 to AndroidOrbitingCamera.State(
        orbitOrigin = v3(0, 40, 0),
        azimuthAngle = 90f,
        verticalAngle = 85f,
        orbitDistance = 400f
    ),
    Angle.Angle6A to AndroidOrbitingCamera.State(
        orbitOrigin = v3(47, 35, 7),
        azimuthAngle = 127f,
        verticalAngle = 5f,
        orbitDistance = 50f
    ),
)