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

package org.wysko.midis2jam2.tools.shotlab

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How Shot Lab picks the next song when the viewer asks for one.
 *
 * The point of "Next song" is to get through a folder of songs quickly, so it should go to songs not yet rated,
 * and never straight back to the song just left.
 */
class SongChoiceTest {
    private val a = File("songs/a.mid").absoluteFile
    private val b = File("songs/b.mid").absoluteFile
    private val c = File("songs/c.mid").absoluteFile

    @Test
    fun `a song not yet rated comes before any that have been`() {
        val ratings = listOf(ratingOf(a), ratingOf(b))
        repeat(20) { seed ->
            assertEquals(c, chooseNextSong(listOf(a, b, c), ratings, previous = null, Random(seed)))
        }
    }

    @Test
    fun `once every song is rated, the one rated least comes next`() {
        val ratings = listOf(ratingOf(a), ratingOf(a), ratingOf(b), ratingOf(c), ratingOf(c))
        repeat(20) { seed ->
            assertEquals(b, chooseNextSong(listOf(a, b, c), ratings, previous = null, Random(seed)))
        }
    }

    @Test
    fun `the song just left is never picked again if there is another`() {
        repeat(20) { seed ->
            val next = chooseNextSong(listOf(a, b), emptyList(), previous = a, Random(seed))
            assertEquals(b, next, "Asking for the next song should move on from the one just left")
        }
        assertEquals(a, chooseNextSong(listOf(a), emptyList(), previous = a), "With only one song, it plays again")
        assertNull(chooseNextSong(emptyList(), emptyList(), previous = null))
    }

    @Test
    fun `ratings saved before song paths were recorded still count, by name`() {
        val old = ratingOf(a).copy(songPath = "")
        repeat(20) { seed ->
            val next = chooseNextSong(listOf(a, b), listOf(old), previous = null, Random(seed))
            assertTrue(next == b, "Song a has an old rating, so the unrated song b should come first")
        }
    }

    private fun ratingOf(song: File) = ShotRating(
        ratedAt = "2026-10-02T12:00:00Z", song = song.name, seed = 1, shotIndex = 0, shotCount = 9, start = 0.0,
        length = 5.0, reason = "interest: #0", section = null, size = "Medium", move = "Static", lens = "Normal",
        subjects = emptyList(), plannedYaw = 0f, plannedPitch = 10f, filmedYaw = 0f, filmedPitch = 10f,
        clearView = 1f, framingEverything = false, fieldOfView = 45f, pacing = "Normal", handheld = false,
        previous = null, stars = 4, tags = emptyList(), comment = "", songPath = song.absolutePath,
    )
}
