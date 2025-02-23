package org.wysko.midis2jam2.instrument.harmonicinstance

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.common.ArcControl
import org.wysko.midis2jam2.jme3ktdsl.control
import org.wysko.midis2jam2.jme3ktdsl.cull

class HarmonicInstanceControl(private val parent: MonophonicInstrument, private val index: Int) : AbstractControl() {
    override fun controlUpdate(tpf: Float) {
        spatial.cullHint = when {
            index == 0 -> true
            calculateVisibility() -> true
            else -> false
        }.cull
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit

    private fun calculateVisibility(): Boolean = spatial.control<ArcControl>()?.let {
        if (it.currentArc != null) {
            return true
        }
        it.collector.prev()?.let { prev ->
            val timeGap = it.collector.peek()?.start?.minus(prev.end) ?: Int.MAX_VALUE
            if (timeGap <= parent.context.sequence.smf.tpq * 2) {
                return true
            }
        }
        return false
    } ?: false
}
