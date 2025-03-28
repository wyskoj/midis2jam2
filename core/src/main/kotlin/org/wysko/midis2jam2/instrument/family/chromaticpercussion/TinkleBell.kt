package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.common.DecayedGlowControl
import org.wysko.midis2jam2.instrument.common.DecayedGlowControl.Companion.yellowGlow
import org.wysko.midis2jam2.instrument.common.OscillatorControl
import org.wysko.midis2jam2.instrument.common.Striker
import org.wysko.midis2jam2.instrument.common.invokeHitControls
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.scale
import org.wysko.midis2jam2.jme3ktdsl.vec3

class TinkleBell(context: PerformanceAppState, events: List<MidiEvent>) : DecayedInstrument(context, events) {
    init {
        repeat(12) { i ->
            node {
                this += context.model("tinkle_bell.j3o").apply {
                    this as Node
                    getChild("bell").apply {
                        addControl(OscillatorControl(frequency = 15f, qFactor = 2f))
                    }
                    (getChild("handle") as Node).getChild(1).addControl(DecayedGlowControl(yellowGlow))
                }
            }.apply {
                loc = vec3(i * -4, 0, 0)
                scale = 1 - (i * 0.02)
                addControl(
                    Striker(
                        context,
                        pitchClassHits(i),
                        Striker.Parameters(visibilityBehavior = Striker.VisibilityBehavior.Always),
                        onStrike = {
                            (getChild("handle") as Node).getChild(1).invokeHitControls(it)
                            this.getChild("bell").invokeHitControls(it)
                        }
                    )
                )
                root += this
            }
        }
    }
}
