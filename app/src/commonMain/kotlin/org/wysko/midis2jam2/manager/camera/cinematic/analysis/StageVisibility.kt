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

package org.wysko.midis2jam2.manager.camera.cinematic.analysis

/**
 * When an instrument is on stage, worked out ahead of time from its notes.
 *
 * This follows the same rules as [org.wysko.midis2jam2.instrument.algorithmic.Visibility]: an instrument appears a
 * little before it plays, stays through short rests, and leaves a little after. The planner needs to know this for
 * the whole of a shot before it commits to filming someone, and asking the instruments themselves about a future
 * time would fire their entry and exit callbacks.
 *
 * @property spans The stretches of time, in seconds, during which the instrument is visible, ascending and disjoint.
 */
class StageVisibility(val spans: List<ClosedFloatingPointRange<Double>>) {

    /** Whether the instrument is on stage at [time]. */
    fun isVisibleAt(time: Double): Boolean = spanContaining(time) != null

    /** Whether the instrument stays on stage for all of [from] to [to]. */
    fun isVisibleThroughout(from: Double, to: Double): Boolean =
        spanContaining(from)?.let { to <= it.endInclusive } ?: false

    private fun spanContaining(time: Double): ClosedFloatingPointRange<Double>? {
        var low = 0
        var high = spans.lastIndex
        while (low <= high) {
            val mid = (low + high) / 2
            val span = spans[mid]
            when {
                time < span.start -> high = mid - 1
                time > span.endInclusive -> low = mid + 1
                else -> return span
            }
        }
        return null
    }

    companion object {
        /** The same timings as [org.wysko.midis2jam2.instrument.algorithmic.Visibility.Parameters]. */
        const val SHOW_BEFORE: Double = 1.0
        const val SHOW_BETWEEN: Double = 7.0
        const val SHOW_AFTER: Double = 2.0

        /** Always on stage, for when instruments are set to always show. */
        val ALWAYS: StageVisibility = StageVisibility(listOf(Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY))

        /** Works out when [subject] is on stage. */
        fun of(subject: SubjectNotes): StageVisibility {
            val spans = mutableListOf<ClosedFloatingPointRange<Double>>()
            var spanStart = 0.0
            var spanEnd = Double.NEGATIVE_INFINITY

            // Struck instruments count rests from the previous onset; sustained ones from the previous note's end.
            var lastEnd = Double.NEGATIVE_INFINITY

            subject.notes.forEach { note ->
                val end = if (subject.isStruck) note.start else note.end
                if (spanEnd == Double.NEGATIVE_INFINITY) {
                    spanStart = note.start - SHOW_BEFORE
                } else if (note.start - lastEnd > SHOW_BETWEEN && note.start - SHOW_BEFORE > lastEnd + SHOW_AFTER) {
                    spans += spanStart..spanEnd
                    spanStart = note.start - SHOW_BEFORE
                }
                lastEnd = maxOf(lastEnd, end)
                spanEnd = lastEnd + SHOW_AFTER
            }
            if (spanEnd != Double.NEGATIVE_INFINITY) spans += spanStart..spanEnd

            return StageVisibility(spans)
        }
    }
}
