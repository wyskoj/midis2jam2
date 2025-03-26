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
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.scene.Axis

class FrenchHorn(
    context: PerformanceAppState,
    events: List<MidiEvent>,
) : MonophonicInstrument(context, events) {
    override fun harmonicInstanceSpatial(): Spatial = context.model("french_horn.j3o").apply {
        this as Node
        addControl(RotateControl(intensity = -0.5f))
        addControl(
            PressedKeyControl(
                "french_horn",
                keySpatialName = {
                    "key-$it"
                },
                keyTransformation = { index, pressed ->
                    rot = if (pressed) {
                        when (index) {
                            0 -> vec3(-25, 0, 0)
                            else -> vec3(0, 0, -30)
                        }
                    } else {
                        vec3(0, 0, 0)
                    }
                }
            )
        )
        getChild("bell").addControl(StretchControl(Axis.Y))
    }
}
