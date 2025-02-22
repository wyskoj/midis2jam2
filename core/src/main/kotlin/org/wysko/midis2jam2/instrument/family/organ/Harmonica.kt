package org.wysko.midis2jam2.instrument.family.organ

import com.jme3.math.FastMath.DEG_TO_RAD
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.particle.SteamPufferControl
import org.wysko.midis2jam2.scene.Axis
import kotlin.math.cos
import kotlin.math.sin

class Harmonica(context: PerformanceAppState, events: List<MidiEvent>) : SustainedInstrument(context, events) {
    init {
        SteamPufferControl.makeForPitchClasses(
            context = context,
            root = root,
            variant = SteamPufferControl.Variant.Harmonica,
            scale = 0.75f,
            behavior = SteamPufferControl.Behavior.Outwards,
            axis = Axis.X,
            isPitchClassPlaying = { isPitchClassPlaying(it) },
            setupTransform = { index, node ->
                val angle = 5 * (index - 5.5) * DEG_TO_RAD
                node.loc = vec3(7.2 * sin(angle), 0, 7.2 * cos(angle))
                node.rot = vec3R(0, angle - 1.57, 0)
            }
        )
        root += context.modelD("harmonica/harmonica.obj", "harmonica/harmonica.png")
    }
}