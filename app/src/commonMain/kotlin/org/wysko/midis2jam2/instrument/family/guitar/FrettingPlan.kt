/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.guitar

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.VirtualCompositePitchBendEvent
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingNote
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfile
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingSolution
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Fretter
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Tuning
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.time.DurationUnit.SECONDS

/**
 * How a fretted instrument plays its part: the [fretting engine][Fretter]'s solution, keyed by the instrument's
 * [TimedArc]s.
 *
 * @property profile The instrument the part was solved for.
 * @property notes The notes the engine placed, in the order of [solution]'s fingerings.
 * @property solution Everything the engine decided, including diagnostics for the live readout.
 */
class FrettingPlan private constructor(
    val profile: FrettingProfile,
    val notes: List<FrettingNote>,
    val solution: FrettingSolution,
    arcs: List<TimedArc>,
) {
    /** Where each note is played. Notes that couldn't be fingered are absent. */
    val positions: Map<TimedArc, FretboardPosition> = buildMap {
        arcs.forEachIndexed { index, arc ->
            solution.fingerings[index]?.let { put(arc, FretboardPosition(it.string, it.fret)) }
        }
    }

    /** The tuning the part is played in. */
    val tuning: Tuning get() = solution.tuning

    /** The capo's fret, or `0` for none. */
    val capo: Int get() = solution.capo

    companion object {
        /**
         * Solves the notes in [events] for [profile]. The tuning is inferred unless [tuning] is given.
         *
         * The arcs are built the same way [org.wysko.midis2jam2.instrument.SustainedInstrument] builds its own;
         * [TimedArc] is a data class, so the instrument's arcs find their positions in [positions].
         */
        fun create(
            context: PerformanceManager,
            events: List<MidiEvent>,
            profile: FrettingProfile,
            tuning: Tuning? = null,
        ): FrettingPlan {
            val arcs = TimedArc.fromNoteEvents(context.sequence, events.filterIsInstance<NoteEvent>())
            val bends = BendTimeline(context, events)
            val notes = arcs.map { arc ->
                val start = arc.startTime.toDouble(SECONDS)
                val end = arc.endTime.toDouble(SECONDS)
                val (up, down) = bends.extremes(start, end)
                FrettingNote(arc.note.toInt(), start, end, arc.velocity.toInt(), up, down)
            }
            return FrettingPlan(profile, notes, Fretter.solve(notes, profile, tuning), arcs)
        }
    }

    /** The channel's pitch bend over time, in semitones, searchable by time. */
    private class BendTimeline(context: PerformanceManager, events: List<MidiEvent>) {
        private val bends = VirtualCompositePitchBendEvent.fromEvents(events).sortedBy { it.tick }
        private val times = DoubleArray(bends.size) { context.sequence.getTimeAtTick(bends[it].tick).toDouble(SECONDS) }

        /** The largest upward and downward bend (both as positive semitones) from [start] until [end]. */
        fun extremes(start: Double, end: Double): Pair<Double, Double> {
            if (bends.isEmpty()) return 0.0 to 0.0
            // The last change at or before the note starts is in effect when it starts.
            var lo = 0
            var hi = times.size - 1
            var first = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (times[mid] <= start) {
                    first = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            var up = 0.0
            var down = 0.0
            fun consider(bend: Double) {
                if (bend > up) up = bend
                if (-bend > down) down = -bend
            }
            if (first >= 0) consider(bends[first].bend)
            var index = first + 1
            while (index < bends.size && times[index] < end) {
                consider(bends[index].bend)
                index++
            }
            return up to down
        }
    }
}
