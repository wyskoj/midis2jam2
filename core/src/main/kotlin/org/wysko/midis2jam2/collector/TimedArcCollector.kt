package org.wysko.midis2jam2.collector

import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.HeapAPQ
import org.wysko.midis2jam2.application.PerformanceAppState
import kotlin.time.Duration

class TimedArcCollector(
    context: PerformanceAppState,
    private var arcs: List<TimedArc>,
    private val releaseCondition: (time: Duration, arc: TimedArc) -> Boolean = { time: Duration, arc: TimedArc ->
        time >= arc.endTime
    },
) : Collector<TimedArc> {

    init {
        context.registerCollector(this)
        arcs = arcs.sortedBy { it.startTime }
    }

    private val _currentArcs: MutableSet<TimedArc> = HashSet()
    val currentArcs: Set<TimedArc> get() = _currentArcs

    private val heap = HeapAPQ<Duration, TimedArc>(Duration::compareTo)
    private var index = 0
    private var lastRemoved: TimedArc? = null

    fun advance(time: Duration) {
        while (index < arcs.size && arcs[index].startTime <= time) {
            val arc = arcs[index]
            heap.insert(arc.endTime, arc)
            _currentArcs.add(arc)
            index++
        }

        while (heap.min()?.let { releaseCondition(time, it.value) } == true) {
            heap.removeMin()?.let {
                _currentArcs.remove(it.value)
                lastRemoved = it.value
            }
        }

    }

    override fun seek(time: Duration) {
        _currentArcs.clear()
        heap.clear()
        index = 0
        lastRemoved = null
        advance(time)
    }

    override fun peek(): TimedArc? = arcs.getOrNull(index)
    override fun prev(): TimedArc? = lastRemoved
}