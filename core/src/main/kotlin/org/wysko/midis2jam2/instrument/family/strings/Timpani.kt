package org.wysko.midis2jam2.instrument.family.strings

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.common.RecoilControl
import org.wysko.midis2jam2.instrument.common.Striker
import org.wysko.midis2jam2.instrument.common.Striker.Companion.makeStriker
import org.wysko.midis2jam2.instrument.common.invokeHitControls
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.midi.pitchClass

class Timpani(context: PerformanceAppState, events: List<MidiEvent>) : DecayedInstrument(context, events) {
    init {
        val timpani = context.model("timpani.j3o").apply { addControl(RecoilControl()) }.also { root += it }
        List(12) { index ->
            context.makeStriker(
                hits = hits.filter { pitchClass(it.note) == index },
                variant = Striker.Variant.Mallet("mallets/xylophone.png"),
                onStrike = timpani::invokeHitControls
            ).apply {
                loc = vec3(1.8 * (index - 5.5), 31, 15)
            }
        }.onEach {
            root += it
        }
    }
}