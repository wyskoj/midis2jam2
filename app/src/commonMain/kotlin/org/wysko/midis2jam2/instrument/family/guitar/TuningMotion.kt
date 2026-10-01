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

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * How a fretted instrument shows its tuning and capo over time, worked out from the song time alone so that seeking
 * and looping just work.
 *
 * Each string is tuned some number of semitones away from the instrument's standard tuning ([offsets]). A string tuned
 * down is slacker, so it vibrates wider and slower; one tuned up is tighter. Just before the instrument's first note,
 * it retunes from standard: the keys turn and the strings glide into their new tension, overshooting slightly and
 * settling as a player's ear would, and the capo slides down the neck and clamps.
 *
 * @property offsets Per string, how many semitones it is tuned from standard (negative is lower).
 * @property capo The capo's fret, or `0` for none.
 * @property firstNote When the instrument's first note starts, in seconds, or `null` if it plays nothing; with
 * nothing to play, it is shown already tuned.
 */
class TuningMotion(val offsets: IntArray, val capo: Int, val firstNote: Double?) {

    /** Whether any string is away from standard. */
    val isRetuned: Boolean = offsets.any { it != 0 }

    /** When the retune starts. */
    val retuneStart: Double? = firstNote?.let { it - RETUNE_LEAD }

    /** When the retune ends. */
    val retuneEnd: Double? = firstNote?.let { it - RETUNE_GAP }

    /** How far through the retune [time] is, from `0` (not started) to `1` (done). */
    fun progress(time: Double): Double {
        val start = retuneStart ?: return 1.0
        val end = retuneEnd ?: return 1.0
        return ((time - start) / (end - start)).coerceIn(0.0, 1.0)
    }

    /** Whether [time] falls inside the retune. */
    fun isRetuning(time: Double): Boolean = progress(time).let { it > 0.0 && it < 1.0 }

    /**
     * How many semitones from standard [string] is at [time]: `0` before the retune, the full offset after it, and in
     * between a glide that overshoots a little and settles.
     */
    fun semitones(string: Int, time: Double): Double {
        val glide = settle(progress(time))
        return if (glide == 0.0) 0.0 else offsets[string] * glide
    }

    /** How tight [string] is at [time], relative to standard. Tension goes as the square of the frequency. */
    fun tension(string: Int, time: Double): Double = 2.0.pow(semitones(string, time) / 6.0)

    /** How fast [string]'s vibration cycles at [time], relative to standard. */
    fun vibrationSpeed(string: Int, time: Double): Double = tension(string, time).pow(SPEED_EXPONENT)

    /** How wide [string] vibrates at [time], relative to standard. */
    fun vibrationWidth(string: Int, time: Double): Double = tension(string, time).pow(-WIDTH_EXPONENT)

    /** Whether [string] rings open at [time] because it is being tuned, rather than because it was played. */
    fun ringsWhileTuning(string: Int, time: Double): Boolean = offsets[string] != 0 && isRetuning(time)

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

    /** How much the capo is squashed onto the neck at [time] as it clamps, from `0` (not at all) up. */
    fun capoSquash(time: Double): Double {
        val p = progress(time)
        if (p <= CAPO_SLIDE || p >= 1.0) return 0.0
        return CAPO_SQUASH * sin(PI * (p - CAPO_SLIDE) / (1 - CAPO_SLIDE))
    }

    companion object {
        /** How long before the first note the retune starts, in seconds. */
        const val RETUNE_LEAD: Double = 1.5

        /** How long before the first note the retune ends, in seconds. */
        const val RETUNE_GAP: Double = 0.3

        /** Exaggerates how much slower a slack string vibrates, so the difference can be seen. */
        const val SPEED_EXPONENT: Double = 1.5

        /** Exaggerates how much wider a slack string vibrates. */
        const val WIDTH_EXPONENT: Double = 0.75

        private const val OVERSHOOT = 1.2
        private const val CAPO_SLIDE = 0.7
        private const val CAPO_START = -0.5
        private const val CAPO_SQUASH = 0.25

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
