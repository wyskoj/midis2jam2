package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.collector.EventCollector
import kotlin.math.sqrt
import kotlin.time.Duration

abstract class DecayedInstrument(context: PerformanceAppState, events: List<MidiEvent>) : Instrument() {
    protected val hits = events.filterIsInstance<NoteEvent.NoteOn>()
    open val collector = EventCollector(context, hits)

    open fun onHit(noteOn: NoteEvent.NoteOn) = Unit

    override fun tick(time: Duration, delta: Duration) {
        collector.advanceCollectAll(time).forEach { onHit(it) }
    }

    companion object {
        fun velocityRamp(velocity: Number): Double = sqrt(velocity.toInt() / 127.0)
    }
}