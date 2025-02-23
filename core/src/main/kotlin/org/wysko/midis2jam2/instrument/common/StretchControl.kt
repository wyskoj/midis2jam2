package org.wysko.midis2jam2.instrument.common

import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.fLerp
import org.wysko.midis2jam2.jme3ktdsl.ancestorControl
import org.wysko.midis2jam2.scene.Axis

class StretchControl(
    private val axis: Axis = Axis.Z,
    private val intensity: Float = 1f,
) : AbstractControl() {
    override fun controlUpdate(tpf: Float) {
        val progress = spatial.ancestorControl<ArcControl>()?.currentProgress ?: 1f
        val stretch = fLerp(1f + intensity, 1f, progress)
        spatial.localScale = Vector3f.UNIT_XYZ.clone().apply { set(axis.componentIndex, stretch) }
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}
