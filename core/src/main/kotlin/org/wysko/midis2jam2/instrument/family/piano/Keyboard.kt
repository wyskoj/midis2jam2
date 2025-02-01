package org.wysko.midis2jam2.instrument.family.piano

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.KeyedInstrument
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.midi.noteNumberToPitch

private val range = 21..108

class Keyboard(context: PerformanceAppState, events: List<MidiEvent>) : KeyedInstrument(context, events, range) {

    override val keys: List<Key> = List(88) {
        KeyboardKey(context, this, it + range.first)
    }

    init {
        root += context.model("PianoCase.obj", "Piano.png")
    }

    override fun keyFromNoteNumber(noteNumber: Int): Key? = keys.getOrNull(noteNumber - range.first)
}

private class KeyboardKey(context: PerformanceAppState, keyboard: Keyboard, noteNumber: Int) : Key(keyboard) {
    init {
        root.run {
            this += context.model(model = "PianoKey_${modelKeyFromNoteNumber(noteNumber)}.obj", texture = "Piano.png")
            loc.x = (noteNumber / 12) * 7f - 35
        }
    }

    private fun modelKeyFromNoteNumber(noteNumber: Int) = when (noteNumber) {
        range.first -> "LowA"
        range.last -> "HighC"
        else -> noteNumberToPitch(noteNumber)
    }
}