package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.instrument.family.piano.Keyboard

object InstrumentAssignment {
    fun PerformanceAppState.makeAssignments(sequence: TimeBasedSequence): List<Instrument> {

        val eventsByDestination = mutableMapOf<Destination, MutableList<MidiEvent>>()

        for (track in sequence.smf.tracks) {
            var port = 0
            for (event in track.events) {
                if (event is MetaEvent.Unknown && event.data.size == 2 && event.data.first() == 0x21.toByte()) {
                    port = event.data.last().toInt()
                }
                if (event is MidiEvent) {
                    eventsByDestination
                        .getOrPut(Destination(event.channel.toInt(), port)) { mutableListOf() }
                        .add(event)
                }
            }
        }

        val instruments = mutableListOf<Instrument>()

        eventsByDestination.forEach { (_, u) ->
            instruments += Keyboard(this, u)
        }


        return instruments
    }


    private data class Destination(
        val channel: Int,
        val port: Int = 0,
    )

    private enum class DestinationState {
        Melody, Rhythm;

        companion object {
            fun default(channel: Int): DestinationState = if (channel == 9) Rhythm else Melody
        }
    }
}