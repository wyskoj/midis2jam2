package org.wysko.midis2jam2.collector

import org.wysko.kmidi.midi.event.Event
import org.wysko.midis2jam2.application.PerformanceAppState
import kotlin.time.Duration

class EventCollector<T : Event>(
    private val context: PerformanceAppState,
    private val events: List<T>,
    private val triggerCondition: (Event, Duration) -> Boolean =
        { event, time -> context.sequence.getTimeOf(event) <= time },
    private val onSeek: (EventCollector<T>) -> Unit = {},
) : Collector<T> {

    init {
        context.registerCollector(this)
    }

    private var index = 0

    fun advanceCollectAll(time: Duration): List<T> {
        val startingIndex = index
        while (index < events.size && triggerCondition(events[index], time)) {
            index++
        }
        return events.subList(startingIndex, index)
    }

    fun advanceCollectOne(time: Duration): T? {
        var advanced = false
        while (index < events.size && triggerCondition(events[index], time)) {
            index++
            advanced = true
        }
        return if (advanced) events[index - 1] else null
    }

    override fun seek(time: Duration) {
        index = events.indexOfFirst { context.sequence.getTimeOf(it) >= time }
        if (index == -1) index = events.size
        onSeek(this)
    }

    /**
     * Returns the immediate next event in the future. If there are no more events, the return is `null`.
     */
    override fun peek(): T? = events.getOrNull(index)

    /**
     * Returns the last elapsed event. If no events have yet elapsed, the return is `null`.
     */
    override fun prev(): T? = events.getOrNull(index - 1)
}
