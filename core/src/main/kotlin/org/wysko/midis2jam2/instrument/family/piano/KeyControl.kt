package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.times
import org.wysko.midis2jam2.mapRangeClamped
import org.wysko.midis2jam2.scene.Axis

open class KeyControl(
    private val rotationAxis: Axis = Axis.X,
    private val invertRotation: Boolean = false,
) : AbstractControl() {
    var state: State = State.Up
    protected var maximumKeyRotation = 10f

    override fun controlUpdate(tpf: Float) {
        applyRotation(tpf)
        setShadowEffect()
    }

    protected open fun applyRotation(tpf: Float) {
        spatial.rot = rotationAxis.identity * when (state) {
            is State.Down -> ((state as State.Down).velocity / 127.0) * maximumKeyRotation
            is State.Up -> interpTo(getRotation(), 0f, tpf, 20f)
        } * if (invertRotation) -1 else 1
    }

    protected open fun setShadowEffect() {
        spatial.depthFirstTraversal {
            if (it is Geometry) {
                it.material.setParam("AOIntensity", mapRangeClamped(getRotation(), 0f, maximumKeyRotation, 1.0f, 0.33f))
            }
        }
    }

    private fun getRotation() = spatial.rot.dot(rotationAxis.identity) * if (invertRotation) -1 else 1

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit


    sealed interface State {
        data object Up : State
        data class Down(val velocity: Int) : State

        companion object {
            fun fromVelocity(velocity: Int): State = if (velocity < 1) Up else Down(velocity)
        }
    }
}