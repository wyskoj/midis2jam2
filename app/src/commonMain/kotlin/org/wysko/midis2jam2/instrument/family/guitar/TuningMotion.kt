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

import kotlin.math.pow

/**
 * How a fretted instrument shows its tuning and capo over time, worked out from the song time alone so that seeking
 * and looping just work.
 *
 * Each string is tuned some number of semitones away from the instrument's standard tuning ([offsets]). A string tuned
 * down is slacker, so it vibrates wider and slower; one tuned up is tighter. Just before the instrument's first note,
 * it retunes from standard the way a player does, one string at a time from the lowest: each key turns, gliding its
 * string into its new tension, overshooting slightly and settling as a player's ear would. The strings stay still. The capo
 * slides down the neck and clamps over the same time.
 *
 * @property offsets Per string, how many semitones it is tuned from standard (negative is lower).
 * @property capo The capo's fret, or `0` for none.
 * @property firstNote When the instrument's first note starts, in seconds, or `null` if it plays nothing; with
 * nothing to play, it is shown already tuned.
 * @property songStart When the song's playback begins, in seconds (before its first note, for the intro); the retune
 * is squeezed in after it when the instrument plays from the start.
 */
class TuningMotion(val offsets: IntArray, val capo: Int, val firstNote: Double?, val songStart: Double = 0.0) {

    /** Whether any string is away from standard. */
    val isRetuned: Boolean = offsets.any { it != 0 }

    /** The strings that are retuned, in the order they are tuned. */
    private val retunedStrings: List<Int> = offsets.indices.filter { offsets[it] != 0 }

    /** When the retune ends. */
    val retuneEnd: Double? = firstNote?.let { it - RETUNE_GAP }

    /** When the retune starts: [RETUNE_LEAD] before the first note, but never before the song can be seen. */
    val retuneStart: Double? = firstNote?.let { maxOf(it - RETUNE_LEAD, songStart + START_MARGIN) }

    /** How far through the retune [time] is, from `0` (not started) to `1` (done). */
    fun progress(time: Double): Double {
        val start = retuneStart ?: return 1.0
        val end = retuneEnd ?: return 1.0
        if (end <= start) return if (time >= end) 1.0 else 0.0
        return ((time - start) / (end - start)).coerceIn(0.0, 1.0)
    }

    /**
     * Whether the instrument should be on stage at [time] to show its retune or capo: from a moment before the retune
     * starts, so it has arrived and settled, until its first note. Instruments are otherwise only shown just before
     * they play, which would hide most of the retune.
     */
    fun isShowing(time: Double): Boolean {
        if (!isRetuned && capo == 0) return false
        val start = retuneStart ?: return false
        return time >= start - SHOW_BEFORE && time <= firstNote!!
    }

    /** Whether [time] falls inside the retune. */
    fun isRetuning(time: Double): Boolean = progress(time).let { it > 0.0 && it < 1.0 }

    /**
     * How many semitones from standard [string] is at [time]: `0` before the retune, the full offset after it, and in
     * between a glide that overshoots a little and settles.
     */
    fun semitones(string: Int, time: Double): Double {
        val glide = settle(stringProgress(string, time))
        return if (glide == 0.0) 0.0 else offsets[string] * glide
    }

    /** How tight [string] is at [time], relative to standard. Tension goes as the square of the frequency. */
    fun tension(string: Int, time: Double): Double = 2.0.pow(semitones(string, time) / 6.0)

    /** How fast [string]'s vibration cycles at [time], relative to standard. */
    fun vibrationSpeed(string: Int, time: Double): Double = tension(string, time).pow(SPEED_EXPONENT)

    /** How wide [string] vibrates at [time], relative to standard. */
    fun vibrationWidth(string: Int, time: Double): Double = tension(string, time).pow(-WIDTH_EXPONENT)

    /** How far through tuning [string] [time] is: the retune is shared out between the retuned strings in turn. */
    fun stringProgress(string: Int, time: Double): Double {
        val turn = retunedStrings.indexOf(string)
        if (turn < 0) return 1.0
        return (progress(time) * retunedStrings.size - turn).coerceIn(0.0, 1.0)
    }

    /**
     * Where the capo is at [time], as a fret position (fractions fall between frets), or `null` while it isn't on the
     * neck. It slides down from beyond the nut over the first part of the retune.
     */
    fun capoFret(time: Double): Double? {
        if (capo == 0) return null
        val p = progress(time)
        if (p <= 0.0) return null
        val slide = smoothstep((p / CAPO_SLIDE).coerceAtMost(1.0))
        return CAPO_START + (capo - CAPO_START) * slide
    }

    /**
     * How far the capo is lifted off the strings at [time], from `1` (while it slides down the neck) to `0` (clamped,
     * which it is by the end of the retune).
     */
    fun capoLift(time: Double): Double {
        val p = progress(time)
        if (p <= CAPO_SLIDE) return 1.0
        return 1 - smoothstep(((p - CAPO_SLIDE) / (1 - CAPO_SLIDE)).coerceAtMost(1.0))
    }

    companion object {
        /** How long before the first note the retune starts, in seconds. */
        const val RETUNE_LEAD: Double = 2.5

        /** How long before the first note the retune ends, in seconds. */
        const val RETUNE_GAP: Double = 0.4

        /** How long the instrument is on stage before its retune starts, in seconds. */
        const val SHOW_BEFORE: Double = 0.6

        /** How soon after the song's playback begins the retune can start, in seconds. */
        const val START_MARGIN: Double = 0.3

        /** Exaggerates how much slower a slack string vibrates, so the difference can be seen. */
        const val SPEED_EXPONENT: Double = 1.5

        /** Exaggerates how much wider a slack string vibrates. */
        const val WIDTH_EXPONENT: Double = 0.75

        private const val OVERSHOOT = 1.2
        private const val CAPO_SLIDE = 0.7
        private const val CAPO_START = -0.5

        private fun smoothstep(x: Double): Double = x * x * (3 - 2 * x)

        /** Eases from `0` to `1`, going a little past `1` (by [OVERSHOOT], as a strength) before settling back on it. */
        internal fun settle(p: Double): Double = when {
            p <= 0.0 -> 0.0
            p >= 1.0 -> 1.0
            else -> {
                // Ease out with overshoot ("ease-out-back"), entered smoothly.
                val x = smoothstep(p) - 1
                1 + (OVERSHOOT + 1) * x * x * x + OVERSHOOT * x * x
            }
        }
    }
}
