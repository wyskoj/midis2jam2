package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.FastMath.PI
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelR
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.common.GlowControl
import org.wysko.midis2jam2.instrument.common.OscillatorControl
import org.wysko.midis2jam2.instrument.common.Striker
import org.wysko.midis2jam2.instrument.common.Striker.Companion.makeStriker
import org.wysko.midis2jam2.instrument.common.invokeHitControls
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.midi.pitchClass
import org.wysko.midis2jam2.scene.Axis

class TubularBells(context: PerformanceAppState, events: List<MidiEvent>) : DecayedInstrument(context, events) {
    private val bells = List(12) {
        context.modelR("tubular_bell.obj", "silver.png").also {
            it.addControl(OscillatorControl(-0.5f, 3.0f, PI / 2, 0.3f))
            it.addControl(GlowControl())
        }
    }.onEachIndexed { index, spatial ->
        spatial.loc = vec3((index - 5) * 4, 0, -4)
        spatial.scale = (-0.04545 * index) + 1
        root += spatial
    }



    init {
        repeat(12) { i ->
            context.makeStriker(
                events.filterIsInstance<NoteEvent.NoteOn>().filter { (it.note + 3) % 12 == i },
                variant = Striker.Variant.TubularBellsMallet,
                onStrike = bells[i]::invokeHitControls,
                parameters = Striker.Parameters(liftAxis = Axis.Z, liftIntensity = -1f)
            ).also {
                it.loc = vec3((i - 5) * 4, -36 + (18 / 11.0) * i, 0)
                root += it
            }
        }
    }

    override fun onHit(noteOn: NoteEvent.NoteOn) {
        super.onHit(noteOn)
        bells[pitchClass(noteOn.note)].invokeHitControls(noteOn.velocity)
    }
}