package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.event.Event
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.dSeconds

object InstrumentVisibility {
    @Suppress("ReturnCount")
    fun sustainedVisibilityRules(collector: TimedArcCollector, time: Float): Boolean {
        if (collector.currentArcs.isNotEmpty()) return true

        collector.peek()?.let {
            if (it.startTime.dSeconds - time <= 1) return true
        }

        collector.prev()?.let { prev ->
            collector.peek()?.let { peek ->
                if (peek.startTime.dSeconds - prev.endTime.dSeconds <= 7) return true
            }
        }

        collector.prev()?.let {
            if (time - it.endTime.dSeconds <= 2) return true
        }

        return false
    }

    @Suppress("ReturnCount")
    fun decayedVisibilityRules(
        instrument: DecayedInstrument,
        time: Double,
        getTime: (Event) -> Double,
    ): Boolean {
        val collector = instrument.collector

        collector.peek()?.let {
            if (getTime(it) - time <= 1) return true
        }

        collector.peek()?.let { peek ->
            collector.prev()?.let { prev ->
                if (getTime(peek) - getTime(prev) <= 7) return true
            }
        }

        collector.prev()?.let {
            if (time - getTime(it) <= 2) return true
        }

        return false
    }
}
