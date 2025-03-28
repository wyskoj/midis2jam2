package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.common.PressedKeyControl
import org.wysko.midis2jam2.instrument.common.RotateControl
import org.wysko.midis2jam2.instrument.common.StretchControl
import org.wysko.midis2jam2.scene.Axis

class Tuba(context: PerformanceAppState, events: List<MidiEvent>) : MonophonicInstrument(context, events) {
    override fun harmonicInstanceSpatial(): Spatial = context.model("tuba.j3o").apply {
        this as Node
        addControl(RotateControl(intensity = 0.5f))
        addControl(PressedKeyControl("tuba"))
        getChild("bell").apply { addControl(StretchControl(Axis.Y)) }
    }
}