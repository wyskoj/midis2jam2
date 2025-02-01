package org.wysko.midis2jam2.instrument.family.piano

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.KeyedInstrument
import org.wysko.midis2jam2.instrument.family.piano.Keyboard.Variant
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.midi.noteNumberToPitch

private val range = 21..108

private const val DEFAULT_PIANO_TEXTURE = "piano.png"

class Keyboard(context: PerformanceAppState, events: List<MidiEvent>, variant: Variant? = null) :
    KeyedInstrument(context, events, range) {

    override val keys: List<Key> = List(88) {
        KeyboardKey(context, this, it + range.first, variant)
    }

    init {
        root += context.model("PianoCase.obj", variant?.texture ?: DEFAULT_PIANO_TEXTURE)
    }

    override fun keyFromNoteNumber(noteNumber: Int): Key? = keys.getOrNull(noteNumber - range.first)

    enum class Variant(internal val texture: String) {
        BrightAcoustic("piano-bright_acoustic.png"),
        Celesta("piano-celesta.png"),
        Electric1("piano-electric1.png"),
        Electric2("piano-electric2.png"),
        ElectricGrand("piano-electric_grand.png"),
        Harpsichord("piano-harpsichord.png"),
        HonkyTonk("piano-honky_tonk.png")
    }
}

private class KeyboardKey(context: PerformanceAppState, keyboard: Keyboard, noteNumber: Int, variant: Variant? = null) :
    Key(keyboard) {
    init {
        root.run {
            this += context.model(
                model = "PianoKey_${modelKeyFromNoteNumber(noteNumber)}.obj",
                variant?.texture ?: DEFAULT_PIANO_TEXTURE
            )
            loc.x = (noteNumber / 12) * 7f - 35
        }
    }

    private fun modelKeyFromNoteNumber(noteNumber: Int) = when (noteNumber) {
        range.first -> "LowA"
        range.last -> "HighC"
        else -> noteNumberToPitch(noteNumber)
    }
}