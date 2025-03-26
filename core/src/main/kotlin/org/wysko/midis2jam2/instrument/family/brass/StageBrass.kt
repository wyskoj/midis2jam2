package org.wysko.midis2jam2.instrument.family.brass

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.application.reflectiveMaterial
import org.wysko.midis2jam2.instrument.DeferredLocationBehavior
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.common.RisingControl
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.times
import org.wysko.midis2jam2.jme3ktdsl.vec3

private val BASE_POSITION = vec3(0f, 29.5, -152.65)

class StageBrass(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    variant: Variant,
) : SustainedInstrument(context, events), DeferredLocationBehavior {
    private val horns = List(12) {
        context.model("stage_horn.j3o").apply {
            setMaterial(context.reflectiveMaterial("common/horn/${variant.name.lowercase()}.png"))
            addControl(RisingControl(context) { pitchClassArcs(it).firstOrNull() })
        }
    }.onEach { root += it }

    override fun applyLocationBehavior(index: Float) {
        horns.forEachIndexed { peepIndex, peep ->
            peep.localTranslation = when {
                index >= 0 -> vec3(0, 16 + 1.5 * peepIndex, 0).quat()
                    .mult(BASE_POSITION.clone() + vec3(0, 3, -5) * index)

                else -> vec3(0, 16 + 1.5 * peepIndex, 0).quat()
                    .mult(BASE_POSITION.clone() + vec3(0, 3, 5) * index)
            }
        }
    }

    enum class Variant {
        Gold, Silver, Copper
    }
}
