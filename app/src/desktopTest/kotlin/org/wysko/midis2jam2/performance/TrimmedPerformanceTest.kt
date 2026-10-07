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

package org.wysko.midis2jam2.performance

import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.midis2jam2.instrument.family.strings.Violin
import org.wysko.midis2jam2.midi.trimSilence
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A performance of a file whose silence has been trimmed.
 *
 * The instruments on stage are chosen from the setup a file does before its first note. Trimming moves all of that
 * setup to the moment the song starts, so it has to keep its order, or the band changes.
 */
class TrimmedPerformanceTest {

    @Test
    @Spec("playback.trim-silence.keeps-setup-messages")
    fun `trimming the silence does not change the instruments on stage`() {
        val untrimmed = MidiFixtures.setupDuringSilentIntro(program = VIOLIN)
        val trimmed = untrimmed.smf.trimSilence().toTimeBasedSequence()

        HeadlessPerformance.start(untrimmed, attachManagers = false).use { performance ->
            val before = performance.assignFor(untrimmed)
            val after = performance.assignFor(trimmed)

            assertTrue(before.any { it is Violin }, "The fixture should play a violin: $before")
            assertEquals(
                before.map { it::class.simpleName },
                after.map { it::class.simpleName },
                "The program chosen during the silence should still choose the instrument once it is trimmed",
            )
        }
    }

    private companion object {
        const val VIOLIN = 40
    }
}
