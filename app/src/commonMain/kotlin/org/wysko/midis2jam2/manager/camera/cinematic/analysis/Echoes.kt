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

import org.wysko.midis2jam2.manager.camera.cinematic.analysis.InterestAnalyzer.isPitched
import kotlin.math.roundToInt

/** The longest delay at which one part can be heard as an echo of another, in seconds. */
private const val MAX_ECHO_DELAY = 0.5

/**
 * The shortest delay at which one part can be heard as an echo of another, in hundredths of a second. Parts that play
 * the same notes together are doubling a line, which is ordinary arranging: each player is still worth watching.
 */
private const val MIN_ECHO_DELAY = 5

/**
 * How many of an echo's notes may also sound with the original, as a fraction of those that repeat it after its
 * delay. More than this, and the two parts are doubling a line, not one echoing the other.
 */
private const val SIMULTANEOUS_SHARE = 0.3

/** How closely an echo's notes must keep to its delay, in seconds. */
private const val DELAY_TOLERANCE = 0.02

/** How many of a part's notes must echo another part's for it to count as an echo. */
private const val ECHO_SHARE = 0.6

/** How few notes a part may have and still be judged: a handful of notes can match anything. */
private const val MIN_NOTES = 16

/**
 * Finds parts that only echo another part.
 *
 * Arrangers fake a delay effect, or thicken a line, by copying a part to another channel: the same notes, or the
 * same notes an octave away, a fraction of a second later. On stage that shows as two players, but musically it is
 * one line. Filmed as two, the camera calls it a duet and dwells on it. An echo is a part most of whose notes repeat
 * another part's notes at one steady delay, and far fewer of them at the same moment.
 *
 * Parts that double a line at the same moment (strings in octaves, a bass doubling the guitar's riff, a piano part
 * that plays everything) are not echoes.
 */
object Echoes {

    /** Every part in [subjects] that echoes another, mapped to the part it echoes. */
    fun find(subjects: List<SubjectNotes>): Map<Int, Int> {
        val echoes = mutableMapOf<Int, Int>()
        // Only pitched parts: a drummer's steady beat matches itself, and any other groove, at many delays.
        val pitched = subjects.filter { it.notes.size >= MIN_NOTES && it.kind.isPitched }
        pitched.forEach { copy ->
            pitched
                .filter { it.id != copy.id && it.id !in echoes }
                .firstOrNull { original -> echoes(copy, original) }
                ?.let { echoes[copy.id] = it.id }
        }
        return echoes
    }

    /**
     * Whether [copy] echoes [original]: most of its notes repeat one of [original]'s at a steady delay, a moment
     * later, rather than with it.
     */
    private fun echoes(copy: SubjectNotes, original: SubjectNotes): Boolean {
        val sources = original.notes.sortedBy { it.start }
        val starts = sources.map { it.start }.toDoubleArray()
        // Every way each of the copy's notes could repeat one of the original's, by delay in hundredths of a second.
        val delays = mutableMapOf<Int, Int>()
        copy.notes.forEach { note ->
            var i = lowerBound(starts, note.start - MAX_ECHO_DELAY - DELAY_TOLERANCE)
            val seen = mutableSetOf<Int>()
            while (i < sources.size && sources[i].start <= note.start + DELAY_TOLERANCE) {
                val source = sources[i]
                if ((note.note - source.note) % 12 == 0) {
                    val key = ((note.start - source.start) * 100).roundToInt()
                    if (seen.add(key)) delays.merge(key, 1, Int::plus)
                }
                i++
            }
        }
        val delay = delays.filterKeys { it >= MIN_ECHO_DELAY }.maxByOrNull { it.value }?.key ?: return false

        // How many of the copy's notes repeat one of the original's, [hundredths] of a second earlier.
        fun repeatedAt(hundredths: Int) = copy.notes.count { note ->
            val at = note.start - hundredths / 100.0
            var i = lowerBound(starts, at - DELAY_TOLERANCE)
            var found = false
            while (!found && i < sources.size && sources[i].start <= at + DELAY_TOLERANCE) {
                found = (note.note - sources[i].note) % 12 == 0
                i++
            }
            found
        }
        val delayed = repeatedAt(delay)
        if (delayed < ECHO_SHARE * copy.notes.size) return false

        // Parts that double a line together also seem to repeat it later, wherever the line repeats a note. An echo
        // is heard after the original, not with it.
        return repeatedAt(0) < SIMULTANEOUS_SHARE * delayed
    }

    private fun lowerBound(sorted: DoubleArray, value: Double): Int {
        var low = 0
        var high = sorted.size
        while (low < high) {
            val mid = (low + high) / 2
            if (sorted[mid] < value) low = mid + 1 else high = mid
        }
        return low
    }
}
