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

package org.wysko.midis2jam2.instrument.family.guitar.fretting

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Renders what the fretting engine decided as fixed-width text: what it inferred for the whole part, and what it
 * is doing at a given moment.
 *
 * Plain ASCII, so it draws in jME's `Console.fnt`. The current slice is found by binary search on every call, so
 * seeking and looping need no bookkeeping.
 */
object FrettingReadout {
    private const val ALTERNATIVES = 3
    private const val BAR_WIDTH = 10

    /** The readout for [solution] at [time] seconds into the song. */
    fun format(solution: FrettingSolution, time: Double): String = buildString {
        appendGlobal(solution)
        appendLocal(solution, time)
    }

    private fun StringBuilder.appendGlobal(solution: FrettingSolution) {
        val global = solution.global
        append(solution.profile.name).append('\n')
        append("tuning ").append(solution.tuning.displayName).append(" [").append(solution.tuning.noteNames).append(']')
        append("  capo ").append(solution.capo).append('\n')
        global.ranking.firstOrNull()?.let { chosen ->
            append("score ").append(f1(chosen.score))
            global.ranking.drop(1).take(ALTERNATIVES - 1).forEach { alternative ->
                append(" | ").append(alternative.label).append(' ').append(f1(alternative.score))
                if (alternative.unplayable > 0) append(" (").append(alternative.unplayable).append(" unplayable)")
            }
            append('\n')
        }
        append("notes ").append(global.noteCount)
        append("  slices ").append(global.sliceCount)
        append("  dropped ").append(global.droppedCount)
        append("  steals ").append(global.stealCount).append('\n')
        append("riffs ").append(global.riffFamilies)
        append("  decode ").append(global.decodeMillis).append(" ms\n")
    }

    private fun StringBuilder.appendLocal(solution: FrettingSolution, time: Double) {
        append("t ").append(clock(time))
        val index = currentSlice(solution.slices, time)
        if (index < 0) {
            append("  (before the first note)\n")
            return
        }
        val slice = solution.slices[index]
        append("  slice ").append(index + 1).append('/').append(solution.slices.size)
        if (time > slice.end) append("  (ended)")
        append('\n')

        val filled = (slice.leadness * BAR_WIDTH).roundToInt().coerceIn(0, BAR_WIDTH)
        append("[").append("#".repeat(filled)).append(".".repeat(BAR_WIDTH - filled)).append("] ")
        append(f2(slice.leadness)).append(if (slice.leadness >= 0.5) " LEAD" else " RHYTHM")
        append("  phrase ").append(if (slice.phraseStart) "start" else "mid").append('\n')

        append("hand ").append(if (slice.hand < 0) "-" else slice.hand.toString())
        append("  barre ").append(if (slice.barreFret < 0) "-" else slice.barreFret.toString())
        append("  shift ").append(if (slice.shift > 0) "+" else "").append(slice.shift)
        if (slice.shift != 0) append(" (urgency ").append(f1(slice.urgency)).append(')')
        append('\n')

        val strings = solution.tuning.size
        append("string")
        for (s in 0 until strings) append("%4s".format(noteName(solution.tuning[s] + solution.capo)))
        append('\n')
        append("fret  ")
        for (s in 0 until strings) append("%4s".format(slice.frets[s].takeIf { it >= 0 }?.toString() ?: "-"))
        append('\n')
        append("finger")
        for (s in 0 until strings) {
            val finger = slice.fingers[s]
            append("%4s".format(if (finger < 0) "-" else if (finger == 0) "o" else finger.toString()))
        }
        append('\n')

        val breakdown = slice.breakdown
        append("static ").append(f2(breakdown.staticTotal))
        breakdown.nonZero(static = true).takeIf { it.isNotEmpty() }?.let { terms ->
            append(" = ").append(terms.joinToString(" ") { (term, value) -> "${term.label} ${f2(value)}" })
        }
        append('\n')
        append("trans  ").append(f2(breakdown.transitionTotal))
        breakdown.nonZero(static = false).takeIf { it.isNotEmpty() }?.let { terms ->
            append(" = ").append(terms.joinToString(" ") { (term, value) -> "${term.label} ${f2(value)}" })
        }
        append('\n')
    }

    /** The index of the last slice starting at or before [time], or `-1` if [time] is before them all. */
    fun currentSlice(slices: List<SliceDiagnostics>, time: Double): Int {
        var lo = 0
        var hi = slices.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (slices[mid].start <= time) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return found
    }

    private fun clock(time: Double): String {
        val clamped = time.coerceAtLeast(0.0)
        val minutes = (clamped / 60).toInt()
        return "%02d:%05.2f".format(Locale.ROOT, minutes, clamped - minutes * 60)
    }

    private fun f1(value: Double) = "%.1f".format(Locale.ROOT, value)

    private fun f2(value: Double) = "%.2f".format(Locale.ROOT, value)
}
