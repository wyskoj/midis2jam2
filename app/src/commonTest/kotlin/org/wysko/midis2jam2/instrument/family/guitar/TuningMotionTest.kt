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

import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How a fretted instrument's tuning and capo are shown over time: slacker strings vibrate wider and slower, the
 * instrument retunes from standard just before its first note, and the capo slides into place.
 *
 * Everything is a function of the song time, so these check values at chosen times rather than stepping frames.
 */
class TuningMotionTest {
    private val firstNote = 10.0
    private val dropD = TuningMotion(intArrayOf(-2, 0, 0, 0, 0, 0), capo = 0, firstNote = firstNote)
    private val standard = TuningMotion(IntArray(6), capo = 0, firstNote = firstNote)
    private val before = firstNote - TuningMotion.RETUNE_LEAD - 1
    private val during = firstNote - (TuningMotion.RETUNE_LEAD + TuningMotion.RETUNE_GAP) / 2
    private val after = firstNote

    @Test
    @Spec("instrument.fretted.tuning.slack")
    fun `a string tuned down vibrates slower and wider, and one in standard tuning is unchanged`() {
        assertTrue(dropD.vibrationSpeed(0, after) < 1.0, "The lowered string should vibrate slower")
        assertTrue(dropD.vibrationWidth(0, after) > 1.0, "The lowered string should vibrate wider")
        (1..5).forEach {
            assertEquals(1.0, dropD.vibrationSpeed(it, after), "String $it isn't retuned")
            assertEquals(1.0, dropD.vibrationWidth(it, after), "String $it isn't retuned")
        }
        listOf(before, during, after).forEach { t ->
            (0..5).forEach {
                assertEquals(1.0, standard.vibrationSpeed(it, t))
                assertEquals(1.0, standard.vibrationWidth(it, t))
            }
        }
        val raised = TuningMotion(intArrayOf(0, 0, 0, 0, 0, 2), capo = 0, firstNote = firstNote)
        assertTrue(raised.vibrationSpeed(5, after) > 1.0, "A string tuned up is tighter")
    }

    @Test
    @Spec("instrument.fretted.tuning.retune")
    fun `the instrument retunes from standard just before its first note`() {
        assertEquals(0.0, dropD.semitones(0, before), "Before the retune the string is at standard")
        assertEquals(-2.0, dropD.semitones(0, after), "After the retune the string is at its tuning")
        assertTrue(dropD.isRetuning(during))
        assertTrue(dropD.ringsWhileTuning(0, during), "The string being tuned rings")
        assertTrue(!dropD.ringsWhileTuning(1, during), "A string that stays in standard doesn't")
        assertTrue(!dropD.ringsWhileTuning(0, after), "Tuning is over by the first note")
        assertTrue(!standard.isRetuned)
    }

    @Test
    fun `retuning overshoots a little and settles`() {
        val curve = (0..100).map { TuningMotion.settle(it / 100.0) }
        assertEquals(0.0, curve.first())
        assertEquals(1.0, curve.last())
        assertTrue(curve.max() in 1.01..1.2, "The glide should pass its target slightly: peak ${curve.max()}")
    }

    @Test
    @Spec("instrument.fretted.capo")
    fun `the capo slides down the neck to its fret during the retune`() {
        val capo = TuningMotion(IntArray(6), capo = 3, firstNote = firstNote)
        assertNull(capo.capoFret(before), "The capo isn't on the neck before the retune")
        val sliding = capo.capoFret(firstNote - TuningMotion.RETUNE_LEAD + 0.3)!!
        assertTrue(sliding < 3.0, "Partway through it is still sliding: $sliding")
        assertEquals(3.0, capo.capoFret(after))
        assertNull(dropD.capoFret(after), "No capo, nothing on the neck")
    }

    @Test
    fun `a part with nothing to play is shown already tuned`() {
        val silent = TuningMotion(intArrayOf(-2, 0, 0, 0, 0, 0), capo = 2, firstNote = null)
        assertEquals(-2.0, silent.semitones(0, 0.0))
        assertEquals(2.0, silent.capoFret(0.0))
    }
}
