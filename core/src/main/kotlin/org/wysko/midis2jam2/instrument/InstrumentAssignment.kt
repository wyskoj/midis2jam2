package org.wysko.midis2jam2.instrument

import com.jme3.math.ColorRGBA
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.FrenchHorn
import org.wysko.midis2jam2.instrument.family.brass.StageBrass
 import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.instrument.family.brass.Trumpet
import org.wysko.midis2jam2.instrument.family.brass.Tuba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TinkleBell
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.Choir
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Timpani

const val DEFAULT_MELODY_BANK = 0x00
const val DEFAULT_RHYTHM_BANK = 0x80

object InstrumentAssignment {
    fun PerformanceAppState.makeAssignments(sequence: TimeBasedSequence): List<Instrument> {
        val eventsByDestination = groupEventsByDestination(sequence)

        val instruments = mutableListOf<Instrument>()
        eventsByDestination.forEach { (destination, events) ->
            var patch = Patch.default(destination.channel)
            val eventsByPatch = mutableMapOf<Patch, MutableList<MidiEvent>>(patch to mutableListOf())
            val patchByNote = mutableMapOf<Byte, Patch>()

            for (event in events) {
                when (event) {
                    is NoteEvent.NoteOff -> eventsByPatch[patchByNote[event.note]]?.add(event)
                    is NoteEvent.NoteOn -> patchByNote[event.note] = patch
                    is ProgramEvent -> patch = patch.copy(program = event.program.toInt())
                    else -> Unit
                }
                eventsByPatch.getOrPut(patch) { mutableListOf() }.add(event)
            }

            eventsByPatch.forEach { (patch, events) ->
                buildInstrument(patch, events)?.let { instruments += it }
            }
        }

        return instruments
    }

    private fun groupEventsByDestination(sequence: TimeBasedSequence): MutableMap<Destination, MutableList<MidiEvent>> {
        val eventsByDestination = mutableMapOf<Destination, MutableList<MidiEvent>>()
        for (track in sequence.smf.tracks) {
            var port = 0
            for (event in track.events) {
                if (event is MetaEvent.Unknown && event.data.size == 2 && event.data.first() == 0x21.toByte()) {
                    port = event.data.last().toInt()
                }
                if (event is MidiEvent) {
                    eventsByDestination.getOrPut(Destination(event.channel.toInt(), port)) { mutableListOf() }
                        .add(event)
                }
            }
        }
        return eventsByDestination
    }

    @Suppress("CyclomaticComplexMethod")
    private fun PerformanceAppState.buildInstrument(patch: Patch, events: List<MidiEvent>): Instrument? =
        when {
            patch.bank != DEFAULT_RHYTHM_BANK ->
                @Suppress("MagicNumber")
                when (patch.program) {
                    0 -> Keyboard(this, events)
                    1 -> Keyboard(this, events, Keyboard.Variant.BrightAcoustic)
                    2 -> Keyboard(this, events, Keyboard.Variant.ElectricGrand)
                    3 -> Keyboard(this, events, Keyboard.Variant.HonkyTonk)
                    4 -> Keyboard(this, events, Keyboard.Variant.Electric1)
                    5 -> Keyboard(this, events, Keyboard.Variant.Electric2)
                    6 -> Keyboard(this, events, Keyboard.Variant.Harpsichord)
                    7 -> Keyboard(this, events, Keyboard.Variant.Clavichord)
                    8 -> Keyboard(this, events, Keyboard.Variant.Celesta)
                    9 -> Mallets(this, events, Mallets.Variant.Glockenspiel)
                    10 -> MusicBox(this, events)
                    11 -> Mallets(this, events, Mallets.Variant.Vibraphone)
                    12 -> Mallets(this, events, Mallets.Variant.Marimba)
                    13 -> Mallets(this, events, Mallets.Variant.Xylophone)
                    14 -> TubularBells(this, events)
                    21 -> Accordion(this, events, Accordion.Variant.Accordion)
                    22 -> Harmonica(this, events)
                    23 -> Accordion(this, events, Accordion.Variant.TangoAccordion)
                    47 -> Timpani(this, events)
                    52 -> Choir(this, events, Choir.Variant.StaticTexture.Standard)
                    53 -> Choir(this, events, Choir.Variant.Custom(207 / 360f))
                    54 -> Choir(this, events, Choir.Variant.Custom(117 / 360f))
                    56 -> Trumpet(this, events, Trumpet.Variant.Standard)
                    57 -> Trombone(this, events)
                    58 -> Tuba(this, events)
                    59 -> Trumpet(this, events, Trumpet.Variant.Muted)
                    60 -> FrenchHorn(this, events)
                    61 -> StageBrass(this, events, StageBrass.Variant.Gold)
                    62 -> StageBrass(this, events, StageBrass.Variant.Silver)
                    63 -> StageBrass(this, events, StageBrass.Variant.Copper)
                    80 -> SpaceLaser(this, events, ColorRGBA.Blue)
                    81 -> SpaceLaser(this, events, ColorRGBA.Yellow)
                    85 -> Choir(this, events, Choir.Variant.Custom(292 / 360f))
                    94 -> Choir(this, events, Choir.Variant.Halo)
                    101 -> Choir(this, events, Choir.Variant.StaticTexture.Goblin)
                    112 -> TinkleBell(this, events)
                    else -> null
                }

            else -> null
        }

    private data class Destination(val channel: Int, val port: Int = 0)

    private data class Patch(val program: Int, val bank: Int = 0) {
        companion object {

            fun default(channel: Int) = Patch(
                program = 0,
                bank = if (channel != 9) DEFAULT_MELODY_BANK else DEFAULT_RHYTHM_BANK
            )
        }
    }
}
