package org.wysko.midis2jam2.scene

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.jme3ktdsl.cull
import kotlin.reflect.KClass

class StandControl(
    private val context: PerformanceAppState,
    private val instrumentClass: KClass<out Instrument>,
) : AbstractControl() {

    override fun controlUpdate(tpf: Float) {
        getSpatial().cullHint = context.instruments.any {
            context.application.stateManager.getState(InstrumentManager::class.java).anyVisible(instrumentClass)
        }.cull
    }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
}