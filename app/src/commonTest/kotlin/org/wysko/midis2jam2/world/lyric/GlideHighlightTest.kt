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

package org.wysko.midis2jam2.world.lyric

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The timing of the glide lyric style's wipe.
 *
 * The wipe itself is drawn by a shader the headless test engine never compiles, so this is the part of
 * it that can go wrong without anyone seeing: how far through the line the wipe should be at each moment.
 */
class GlideHighlightTest {

    /** "Twin" at 0 s, "kle " at 0.5 s, "star" at 1 s: four characters each. */
    private val line = listOf(
        TimedSyllable(0.seconds, 4),
        TimedSyllable(500.milliseconds, 4),
        TimedSyllable(1.seconds, 4),
    )

    @Test
    fun `nothing is sung before the first syllable`() {
        assertEquals(0f, glidedCharacters(line, (-1).seconds))
    }

    @Test
    fun `a syllable glides from its start to the start of the next`() {
        assertEquals(0f, glidedCharacters(line, 0.seconds))
        assertEquals(2f, glidedCharacters(line, 250.milliseconds), "Halfway through the first syllable")
        assertEquals(4f, glidedCharacters(line, 500.milliseconds), "The first syllable is done when the next starts")
        assertEquals(7f, glidedCharacters(line, 875.milliseconds))
    }

    @Test
    fun `the last syllable glides for the longest a glide may take`() {
        assertEquals(10f, glidedCharacters(line, 1.seconds + MAX_GLIDE / 2))
        assertEquals(12f, glidedCharacters(line, 1.seconds + MAX_GLIDE))
        assertEquals(12f, glidedCharacters(line, 10.seconds), "The line stays fully sung")
    }

    @Test
    fun `the last syllable finishes by the time the display moves on to the next line`() {
        // The display hands off to the next line half a second after "star" starts: sooner than the cap.
        val handOff = 1.5.seconds

        assertEquals(10f, glidedCharacters(line, 1.25.seconds, handOff), "Halfway between \"star\" and the hand-off")
        assertEquals(12f, glidedCharacters(line, handOff, handOff), "\"star\" should be fully sung at the hand-off")
    }

    @Test
    fun `a hand-off later than the cap doesn't stretch the last syllable`() {
        assertEquals(12f, glidedCharacters(line, 1.seconds + MAX_GLIDE, handOff = 10.seconds))
    }

    @Test
    fun `a syllable before a long rest doesn't glide for the whole rest`() {
        val rest = listOf(TimedSyllable(0.seconds, 4), TimedSyllable(10.seconds, 4))

        assertEquals(4f, glidedCharacters(rest, MAX_GLIDE), "The first syllable should finish within the cap")
        assertEquals(4f, glidedCharacters(rest, 5.seconds))
    }

    @Test
    fun `syllables sung at the same moment are sung at once`() {
        val together = listOf(TimedSyllable(0.seconds, 3), TimedSyllable(0.seconds, 3), TimedSyllable(1.seconds, 2))

        assertEquals(3f, glidedCharacters(together, 0.seconds))
        assertEquals(4.5f, glidedCharacters(together, 500.milliseconds))
    }
}
