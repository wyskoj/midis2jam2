package org.wysko.midis2jam2.instrument.common

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.jme3ktdsl.ancestorControl
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.scene.Axis

private const val DEFAULT_INTENSITY = -10f

class RotateControl(
    private val axis: Axis = Axis.X,
    private val intensity: Float = 1f,
) : AbstractControl() {
    override fun controlUpdate(tpf: Float) {
        val factor = 1 - (spatial.ancestorControl<ArcControl>()?.currentProgress ?: 1f)
        spatial.rot = axis.identity.mult(intensity * factor * DEFAULT_INTENSITY)
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}
