package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.instrument.KeyedInstrument
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.midi.noteNumberToPitch

private val range = 21..108

private const val DEFAULT_PIANO_TEXTURE = "piano/piano.png"

class Keyboard(context: PerformanceAppState, events: List<MidiEvent>, variant: Variant? = null) :
    KeyedInstrument(context, events, range) {

    override val keys: List<Spatial> = List(88) { i ->
        context.modelD(
            model = "PianoKey_${modelKeyFromNoteNumber(i + range.first)}.obj",
            texture = variant?.texture ?: DEFAULT_PIANO_TEXTURE
        )
            .apply { addControl(KeyControl()) }
            .also {
                it.loc.x = (i + range.first) / 12 * 7f - 35
                root += it
            }
    }

    init {
        root += context.modelD("PianoCase.obj", variant?.texture ?: DEFAULT_PIANO_TEXTURE)
    }

    override fun keyFromNoteNumber(noteNumber: Int): Spatial? = keys.getOrNull(noteNumber - range.first)

    enum class Variant(internal val texture: String) {
        BrightAcoustic("piano/bright_acoustic.png"),
        Celesta("piano/celesta.png"),
        Clavichord("piano/clavichord.png"),
        Electric1("piano/electric1.png"),
        Electric2("piano/electric2.png"),
        ElectricGrand("piano/electric_grand.png"),
        Harpsichord("piano/harpsichord.png"),
        HonkyTonk("piano/honky_tonk.png")
    }

    private fun modelKeyFromNoteNumber(noteNumber: Int) = when (noteNumber) {
        range.first -> "LowA"
        range.last -> "HighC"
        else -> noteNumberToPitch(noteNumber)
    }
}