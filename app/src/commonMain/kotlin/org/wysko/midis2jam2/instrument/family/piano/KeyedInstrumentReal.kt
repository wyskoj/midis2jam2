/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.util.control
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class KeyedInstrumentReal(context: PerformanceManager, events: List<MidiEvent>, protected val range: ClosedRange<Int>) :
    SustainedInstrument(context, events) {
    override val collector: TimedArcCollector = TimedArcCollector(context, timedArcs) { time: Duration, arc: TimedArc ->
        when {
            arc.duration > 0.5.seconds -> time >= arc.endTime - 0.1.seconds
            arc.duration > 0.2.seconds -> time >= arc.endTime - 0.05.seconds
            else -> time >= arc.startTime + (arc.duration * 0.5)
        }
    }
    protected abstract val keys: List<Spatial>

    abstract fun keyFromNoteNumber(note: Int): Spatial?

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        for (noteNumber in range.start..range.endInclusive) {
            keyFromNoteNumber(noteNumber)?.control<KeyControl>()?.state = KeyControl.State.fromVelocity(
                isNotePlaying(noteNumber)?.noteOn?.velocity?.toInt() ?: 0
            )
        }
    }

    protected open fun isNotePlaying(noteNumber: Int) = collector.currentTimedArcs.find { it.note.toInt() == noteNumber }
}