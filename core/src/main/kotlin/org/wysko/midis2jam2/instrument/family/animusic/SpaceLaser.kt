package org.wysko.midis2jam2.instrument.family.animusic

import com.jme3.math.ColorRGBA
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.common.ArcControl
import org.wysko.midis2jam2.jme3ktdsl.ancestorControl
import org.wysko.midis2jam2.jme3ktdsl.cull
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.vec3
import kotlin.math.exp
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

class SpaceLaser(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    private val variant: ColorRGBA,
) : MonophonicInstrument(context, events) {
    private val earlyReleases = arcs.associateWith {
        when {
            it.duration > 0.4.seconds -> 0.1.seconds
            it.duration > 0.2.seconds -> 0.08.seconds
            it.duration > 0.1.seconds -> 0.05.seconds
            it.duration > 0.05.seconds -> 0.025.seconds
            else -> 0.02.seconds
        }
    }

    override val createCollector: (context: PerformanceAppState, arcs: List<TimedArc>) -> TimedArcCollector =
        { context, arcs ->
            TimedArcCollector(context, arcs) { time, arc ->
                time >= arc.endTime - earlyReleases[arc]!!
            }
        }

    init {
        root += context.model("space_laser/base.j3o")
    }

    override fun harmonicInstanceSpatial(): Spatial {
        return node {
            this += context.model("space_laser/emitter.j3o").apply {
                ((this as Node).getChild("emitter_nub") as Geometry).material.setColor("GlowColor", variant)
            }
            this += context.model("space_laser/beam.j3o").apply {
                (this as Geometry).material.setColor("GlowColor", variant)
                addControl(BeamControl())
                shadowMode = RenderQueue.ShadowMode.Off
            }
        }.apply {
            addControl(EmitterControl())
        }
    }

    private inner class EmitterControl : AbstractControl() {
        private var wobbleTime = 0f
        private var wobbleIntensity = 0f
        private var rotation = 0.0f

        override fun controlUpdate(tpf: Float) {
            spatial.ancestorControl<ArcControl>()?.let { arcControl ->
                arcControl.currentArc?.let { arc ->
                    wobbleTime += tpf
                    wobbleIntensity = (wobbleTime - 0.1f).coerceIn(0.0f..0.07f)
                    rotation = calculateRotation(arc.note)
                } ?: run {
                    arcControl.collector.peek()?.let {
                        val time = context.time
                        val startTime = it.startTime.toDouble(SECONDS)
                        if (startTime - time <= 1f) { // Less than 1 second away from playing
                            val targetPos = calculateRotation(it.note)
                            if (startTime - time >= tpf) {
                                // Slowly inch our way to the target rotation
                                rotation += ((targetPos - rotation) / (startTime - time) * tpf).toFloat()
                            }
                        }
                    }
                    wobbleTime = 0f
                }
            }
            spatial.rot = vec3(0, 0, rotation + sin(50 * wobbleTime) * wobbleIntensity)
        }

        private fun calculateRotation(note: Byte) = -(1 / (1 + exp(-(note - 64) / 16f)) * 208 - 104)

        override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
    }

    private class BeamControl : AbstractControl() {
        override fun controlUpdate(tpf: Float) {
            spatial.ancestorControl<ArcControl>()?.let { arcControl ->
                setVisibility(arcControl)
            }
        }

        private fun setVisibility(arcControl: ArcControl) {
            spatial.cullHint = (arcControl.currentArc != null).cull
        }

        override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
    }
}
