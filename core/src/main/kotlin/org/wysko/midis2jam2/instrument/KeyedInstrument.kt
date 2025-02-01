package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.instrument.family.piano.Key
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class KeyedInstrument(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    private val range: ClosedRange<Int>,
) : SustainedInstrument(context, events) {

    override val collector = TimedArcCollector(context, arcs) { time: Duration, arc: TimedArc ->
        when {
            arc.duration > 0.5.seconds -> time >= arc.endTime - 0.1.seconds
            arc.duration > 0.2.seconds -> time >= arc.endTime - 0.05.seconds
            else -> time >= arc.startTime + (arc.duration * 0.5)
        }
    }
    protected abstract val keys: List<Key>

    abstract fun keyFromNoteNumber(noteNumber: Int): Key?

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        keys.forEach { it.tick(delta) }
        for (noteNumber in range.start..range.endInclusive) {
            keyFromNoteNumber(noteNumber)?.setState(
                Key.State.fromVelocity(
                    collector.currentArcs.find { it.note.toInt() == noteNumber }?.noteOn?.velocity?.toInt() ?: 0
                )
            )
        }
    }
}