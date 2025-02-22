package org.wysko.midis2jam2.scene

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.jme3ktdsl.cull
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.root
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.scale
import org.wysko.midis2jam2.jme3ktdsl.vec3
import kotlin.reflect.KClass

class StandControl(
    private val context: PerformanceAppState,
    private val instrumentClass: KClass<out Instrument>,
) : AbstractControl() {

    override fun controlUpdate(tpf: Float) {
        spatial.cullHint = context.instruments.any {
            context.application.stateManager.getState(InstrumentManager::class.java).anyVisible(instrumentClass)
        }.cull
    }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

    companion object {
        fun PerformanceAppState.setupStands() {
            root += modelD("stand-piano.obj", "common/rubber_foot.png").apply {
                loc = vec3(-50, 32, -6)
                rot = vec3(0, 45, 0)
                addControl(StandControl(this@setupStands, Keyboard::class))
            }

            root += modelD("stand-mallets.obj", "common/rubber_foot.png").apply {
                loc = vec3(-25, 22.2, 23)
                rot = vec3(0, 33.7, 0)
                scale = 2 / 3.0
                addControl(StandControl(this@setupStands, Mallets::class))
            }
        }
    }
}
