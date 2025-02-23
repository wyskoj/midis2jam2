package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.common.PressedKeyControl
import org.wysko.midis2jam2.instrument.common.RotateControl
import org.wysko.midis2jam2.instrument.common.StretchControl

class Trumpet(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    private val variant: Variant,
) : MonophonicInstrument(context, events) {

    override fun harmonicInstanceSpatial() = context.model("trumpet/${variant.model}").apply {
        (this as Node).getChild("bell_assembly").addControl(StretchControl())
        addControl(RotateControl())
        addControl(PressedKeyControl("trumpet"))
    }

    enum class Variant(internal val model: String) {
        Standard("trumpet.j3o"), Muted("trumpet-muted.j3o")
    }
}
