package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.DeferredLocationBehavior
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.common.RisingControl
import org.wysko.midis2jam2.instrument.common.SustainedGlowControl
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.times
import org.wysko.midis2jam2.jme3ktdsl.vec3

private val BASE_POSITION = vec3(0, 29.5, -152.7)

class Choir(context: PerformanceAppState, events: List<MidiEvent>, variant: Variant = Variant.StaticTexture.Standard) :
    SustainedInstrument(context, events), DeferredLocationBehavior {

    private val peeps = List(12) {
        node {
            this += when (variant) {
                is Variant.StaticTexture -> context.model("choir/peep.j3o", texture = variant.texture)
                is Variant.Custom -> context.model("choir/peep.j3o").apply {
                    (this as Geometry).material.setFloat("HueShift", variant.hue)
                }

                is Variant.Halo -> context.model("choir/peep-halo.j3o").apply {
                    this as Node
                    children.first().addControl(
                        SustainedGlowControl(
                            context, ColorRGBA.Yellow
                        ) { pitchClassArcs(it).firstOrNull() }
                    )
                }
            }.apply {
                rot = vec3(0, 11.27 + it * -5.636, 0)
                addControl(RisingControl(context) { pitchClassArcs(it).firstOrNull() })
            }
        }
    }.onEach { root += it }

    override fun applyLocationBehavior(index: Float) {
        peeps.forEachIndexed { peepIndex, peep ->
            peep.localTranslation = when {
                index >= 0 -> vec3(0, 11.27 + -5.636 * peepIndex, 0).quat()
                    .mult(BASE_POSITION.clone() + vec3(0, 10, -15) * index)

                else -> vec3(0, 11.27 + -5.636 * peepIndex, 0).quat()
                    .mult(BASE_POSITION.clone() + vec3(0, 10, 10) * index)
            }
        }
    }

    sealed interface Variant {
        enum class StaticTexture(internal val texture: String) : Variant {
            Standard("choir/standard.png"), Goblin("choir/goblin.png"),
        }

        data object Halo : Variant
        data class Custom(val hue: Float) : Variant
    }
}
