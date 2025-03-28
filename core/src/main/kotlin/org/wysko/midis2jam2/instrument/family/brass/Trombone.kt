package org.wysko.midis2jam2.instrument.family.brass

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.application.resource
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.fSeconds
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.common.ArcControl
import org.wysko.midis2jam2.instrument.common.RotateControl
import org.wysko.midis2jam2.instrument.common.StretchControl
import org.wysko.midis2jam2.jme3ktdsl.ancestorControl
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.vec3
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

private typealias SlideTable = Map<Byte, List<Int>>

class Trombone(context: PerformanceAppState, events: List<MidiEvent>) : MonophonicInstrument(context, events) {
    override fun harmonicInstanceSpatial(): Spatial = context.model("trombone.j3o").apply {
        this as Node
        addControl(RotateControl(intensity = 0.5f))
        getChild("bell").addControl(StretchControl())
        getChild("slide").addControl(SlideControl())
    }

    private inner class SlideControl : AbstractControl() {
        private val slidePosition: Float
            get() = 0.3f * (spatial.loc.z + 1)

        override fun controlUpdate(tpf: Float) {
            val time = context.time

            spatial.ancestorControl<ArcControl>()?.let { arcControl ->
                if (arcControl.currentArc == null) {
                    advanceSlide(arcControl.collector, time, tpf)
                    return
                }

                arcControl.collector.peek()?.let { peek ->
                    if (peek.startTime.fSeconds - time < 0.1 && arcControl.currentArc?.duration!! >= 0.15.seconds) {
                        advanceSlide(arcControl.collector, time, tpf)
                    } else {
                        snapSlide(arcControl.currentArc)
                    }
                }
            }
        }

        private fun snapSlide(currentArc: TimedArc?) {
            moveToPosition(findSlidePosition(currentArc?.note ?: return).toFloat())
        }

        private fun advanceSlide(collector: TimedArcCollector, time: Float, tpf: Float) {
            collector.peek()?.let { peek ->
                when {
                    peek.note !in 21..80 -> return
                    peek.startTime.fSeconds - time in tpf..1.0f -> {
                        val step =
                            (findSlidePosition(peek.note) - slidePosition) / (peek.startTime.fSeconds - time) * tpf
                        moveToPosition(slidePosition + step)
                    }
                }
            }
        }

        private fun findSlidePosition(note: Byte): Int {
            val positions = slideTable.getOrDefault(note, listOf())
            positions.singleOrNull()?.let { return it }

            return positions.associateWith { abs(slidePosition - it) }.minBy { it.value }.key
        }

        private fun moveToPosition(position: Float) {
            spatial.loc = computeSlideLocation(position.coerceIn(0.5f..7.0f))
        }

        private fun computeSlideLocation(position: Float): Vector3f =
            vec3(0f, 0f, 3.33 * position - 1)

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
    }

    companion object {
        private val slideTable = Yaml.default.decodeFromStream<SlideTable>(resource("/instrument/Trombone.yml"))
    }
}
