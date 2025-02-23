package org.wysko.midis2jam2.instrument.common

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.seconds

class ArcControl(private val context: PerformanceAppState, arcs: List<TimedArc>) : AbstractControl() {
    internal val collector = TimedArcCollector(context, arcs)

    val currentArc: TimedArc?
        get() = collector.currentArcs.firstOrNull()

    val currentProgress: Float?
        get() = currentArc?.calculateProgress(context.time.seconds)?.toFloat()

    override fun controlUpdate(tpf: Float) {
        collector.advance(context.time.seconds)
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}
