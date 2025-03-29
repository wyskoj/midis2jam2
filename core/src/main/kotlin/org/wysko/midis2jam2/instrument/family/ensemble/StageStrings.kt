package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.diffuseMaterial
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.common.AnimatedStringControl
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.cull
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.mapRangeClamped
import org.wysko.midis2jam2.seconds
import kotlin.math.sin

class StageStrings(
    private val context: PerformanceAppState,
    events: List<MidiEvent>,
    private val variant: Variant,
    private val behavior: Behavior = Behavior.Normal,
) : SustainedInstrument(context, events) {

    init {
        repeat(12) { i ->
            root += context.model("stage_string.j3o").apply {
                this as Node
                addControl(StringControl(i))
                getChild("string-anim").addControl(AnimatedStringControl())
                context.diffuseMaterial(variant.texture).let { material ->
                    (getChild("holder") as Geometry).material = material
                    ((getChild("bow") as Node).getChild(1) as Geometry).material = material
                }
            }
        }
    }

    private inner class StringControl(private val i: Int) : AbstractControl() {
        private var pressFactor = 0f

        override fun controlUpdate(tpf: Float) {
            with(spatial as Node) {
                val currentArc = pitchClassArcs(i).firstOrNull()

                getChild("string-idle").cullHint = (currentArc == null).cull
                getChild("string-anim").cullHint = (currentArc != null).cull

                with(getChild("bow")) {
                    loc = vec3(0, 48, 0) + vec3(0, 0, -60).quat()
                        .mult(
                            when (behavior) {
                                Behavior.Normal -> vec3(
                                    0,
                                    8 * ((currentArc?.calculateProgress(context.time.seconds) ?: 0.0) - 0.5),
                                    0
                                )

                                Behavior.Tremolo -> vec3(0, sin(30 * context.time) * 4, 0)
                            }
                        )
                    cullHint = (currentArc != null).cull
                }

                spatial.loc =
                    vec3(0, 2 * i, 0) + vec3(0, 0.9 * i, 0).quat()
                        .mult(vec3(0, 0, updatePress(currentArc != null, tpf)))
            }
        }

        private fun updatePress(isPlaying: Boolean, tpf: Float): Float {
            pressFactor = interpTo(pressFactor, if (isPlaying) 1f else 0f, tpf, 15)
            return mapRangeClamped(pressFactor, 0, 1, -155, -153)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
    }

    enum class Behavior {
        Normal, Tremolo
    }

    enum class Variant(internal val texture: String) {
        Wood1("wood.png"), Wood2("wood-alt.png")
    }
}
