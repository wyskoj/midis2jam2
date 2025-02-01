package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.collector.TimedArcCollector
import kotlin.time.Duration

abstract class SustainedInstrument(context: PerformanceAppState, events: List<MidiEvent>) : Instrument() {
    protected val arcs = TimedArc.fromNoteEvents(context.sequence, events.filterIsInstance<NoteEvent>())
    open val collector = TimedArcCollector(context, arcs)

    override fun tick(time: Duration, delta: Duration) {
        collector.advance(time)
    }
}