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
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.util.plus
import org.wysko.midis2jam2.util.times
import org.wysko.midis2jam2.util.v3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.DurationUnit

class AndroidOrbitingCamera(private val context: Midis2jam2) {
    private var orbitOrigin = v3(-2, 35, -30)
    private var orbitOriginInterpolated = orbitOrigin.clone()
    private var azimuthAngle = 90f
    private var verticalAngle = 18.44
    private var orbitDistance = 150.0f

    private var debugText = BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
        size = 32f
    }

    init {
        debugText.run {
            context.app.guiNode.attachChild(this)
            localTranslation = Vector3f(100f, context.app.viewPort.camera.height - 100f, 0f)
        }
    }

    fun tick(delta: Duration) {
        val fDelta = delta.toDouble(DurationUnit.SECONDS).toFloat()


        context.app.camera.location.interpolateLocal(calculateCameraPosition(), fDelta * 10f)

        orbitOriginInterpolated.interpolateLocal(orbitOrigin, fDelta * 10f)
        context.app.camera.lookAt(orbitOriginInterpolated, Vector3f.UNIT_Y)

        debugText.text = """
            OO: $orbitOrigin
            AZ: $azimuthAngle
            VA: $verticalAngle
            SD: $orbitDistance
        """.trimIndent()
    }

    fun zoom(delta: Float) {
        // Make zoom more sensitive when closer (smaller orbitDistance)
        val zoomSensitivity = (orbitDistance / 400f).coerceIn(0.1f, 1.0f)
        orbitDistance += delta * 200f * zoomSensitivity
        orbitDistance = orbitDistance.coerceIn(5f, 400f)
    }

    fun orbit(deltaX: Float, deltaY: Float) {
        azimuthAngle += deltaX * 0.1f
        verticalAngle += deltaY * 0.1f
    }

    fun pan(x: Float, y: Float) {
        val camRot = context.app.camera.rotation

        val right = camRot.getRotationColumn(0)
        val up = camRot.getRotationColumn(1)

        val horizontalMove = right.mult(x * 0.1f)
        val verticalMove = up.mult(y * 0.1f)

        val totalMove = horizontalMove + verticalMove

        orbitOrigin.addLocal(totalMove)
    }

    private fun calculateCameraPosition(): Vector3f {
        val x = orbitDistance * cos(Math.toRadians(azimuthAngle.toDouble()).toFloat()) * cos(
            Math.toRadians(verticalAngle).toFloat()
        )
        val y = orbitDistance * sin(Math.toRadians(verticalAngle).toFloat())
        val z = orbitDistance * sin(Math.toRadians(azimuthAngle.toDouble()).toFloat()) * cos(
            Math.toRadians(verticalAngle).toFloat()
        )
        return orbitOrigin + v3(x, y, z)
    }
}
