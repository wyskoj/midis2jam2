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

package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.util.interpTo
import org.wysko.midis2jam2.util.mapRangeClamped
import org.wysko.midis2jam2.util.rotR
import org.wysko.midis2jam2.util.times
import org.wysko.midis2jam2.world.Axis

open class KeyControl(
    private val rotationAxis: Axis = Axis.X,
    private val invertRotation: Boolean = false,
    private val color: Key.Color,
) : AbstractControl() {
    var state: State = State.Up
    private var maximumKeyRotation = 10f
    private val maxAOIntensity
        get() = if (color == Key.Color.White) 0.33f else 1.66f

    override fun controlUpdate(tpf: Float) {
        applyRotation(tpf)
        setShadowEffect()
    }

    protected open fun applyRotation(tpf: Float) {
        spatial.rotR = rotationAxis.identity * when (state) {
            is State.Down -> ((state as State.Down).velocity / 127.0) * maximumKeyRotation
            is State.Up -> interpTo(getRotation(), 0f, tpf, 20f)
        } * if (invertRotation) -1 else 1
    }

    protected open fun setShadowEffect() {
        spatial.depthFirstTraversal {
            if (it is Geometry) {
                it.material.setFloat(
                    "AOIntensity",
                    mapRangeClamped(
                        getRotation(),
                        0f,
                        maximumKeyRotation,
                        1.0f,
                        maxAOIntensity
                    )
                )
            }
        }
    }

    private fun getRotation(): Float = (spatial.rotR.dot(rotationAxis.identity) * if (invertRotation) -1 else 1)

    override fun controlRender(rm: RenderManager?, vp: ViewPort?): Unit = Unit

    sealed interface State {
        data object Up : State
        data class Down(val velocity: Int) : State

        companion object {
            fun fromVelocity(velocity: Int): State = if (velocity < 1) Up else Down(velocity)
        }
    }
}