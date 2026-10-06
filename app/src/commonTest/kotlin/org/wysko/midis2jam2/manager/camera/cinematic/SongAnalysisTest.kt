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

package org.wysko.midis2jam2.manager.camera.cinematic

import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.comping
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.drums
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.grid
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.melody
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.pads
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.time
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.Echoes
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.MomentKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.NoteSample
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SectionRole
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SongAnalysis
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.StageVisibility
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectNotes
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * How the cinematic camera reads a song: which parts are interesting, where the song changes, and which moments
 * deserve a shot.
 *
 * These judgements decide who the camera points at. If they go wrong, the camera lingers on a sustained pad while
 * the saxophonist takes a solo.
 */
class SongAnalysisTest {

    @Test
    fun `a soloist outranks the band accompanying them`() {
        val analysis = SongAnalysis.of(
            listOf(
                melody(SOLOIST, 32..63),
                pads(PADS, 0..63, length = 4),
                drums(DRUMS, 0..63),
            ),
            grid(64),
        )

        val soloist = analysis.meanInterest(SOLOIST, 36..63)
        val pads = analysis.meanInterest(PADS, 36..63)
        val drums = analysis.meanInterest(DRUMS, 36..63)
        assertTrue(soloist > pads, "The soloist ($soloist) should be more interesting than the pads ($pads)")
        assertTrue(soloist > drums, "The soloist ($soloist) should be more interesting than the groove ($drums)")

        val solo = analysis.moments.filter { it.kind == MomentKind.Solo && it.subjects == listOf(SOLOIST) }
        assertTrue(
            solo.any { it.start <= time(40) && it.end >= time(56) },
            "The solo from beat 32 should be found as a solo moment, but the moments were ${analysis.moments}"
        )
    }

    @Test
    fun `a part coming in after a long rest is an entrance`() {
        val analysis = SongAnalysis.of(
            listOf(
                melody(LATECOMER, 40..63),
                comping(KEYS, 0..63),
            ),
            grid(64),
        )

        val features = analysis.features.getValue(LATECOMER)
        assertEquals(1f, features.entrance[40], "The first beat back should carry the full entrance spike")
        assertTrue(
            analysis.meanInterest(LATECOMER, 40..41) > analysis.meanInterest(LATECOMER, 56..63),
            "Interest should spike on the entrance and settle afterwards"
        )
        assertTrue(
            analysis.moments.any { it.kind == MomentKind.Entrance && it.subjects == listOf(LATECOMER) &&
                abs(it.start - time(40)) < 1e-6 },
            "The entrance at beat 40 should be a moment, but the moments were ${analysis.moments}"
        )
    }

    @Test
    fun `parts that start the song together are not each an entrance`() {
        val analysis = SongAnalysis.of(listOf(comping(KEYS, 0..31), drums(DRUMS, 0..31)), grid(32))

        assertTrue(
            analysis.moments.none { it.kind == MomentKind.Entrance },
            "The band starting together is the start of the song, not a string of entrances"
        )
    }

    @Test
    fun `a burst of toms in the groove is a fill`() {
        val analysis = SongAnalysis.of(listOf(drums(DRUMS, 0..31, fillBeats = setOf(15))), grid(32))

        val fill = analysis.features.getValue(DRUMS).fill
        assertTrue(fill[15] >= 0.4f, "The tom run on beat 15 should be read as a fill, but scored ${fill[15]}")
        assertTrue((0..14).all { fill[it] == 0f }, "The plain groove should never read as a fill")
        assertTrue(
            analysis.moments.any { it.kind == MomentKind.Fill && abs(it.start - time(15)) < 1e-6 },
            "The fill should be a moment, but the moments were ${analysis.moments}"
        )
    }

    @Test
    fun `long held chords are less interesting than an active part`() {
        val analysis = SongAnalysis.of(
            listOf(
                pads(PADS, 0..31, length = 8),
                comping(KEYS, 0..31),
            ),
            grid(32),
        )

        val pads = analysis.meanInterest(PADS, 4..31)
        val keys = analysis.meanInterest(KEYS, 4..31)
        assertTrue(pads < keys, "Held pads ($pads) should score below a part playing every beat ($keys)")
    }

    @Test
    fun `a quiet passage, a loud one and a quiet one are three sections`() {
        val analysis = SongAnalysis.of(
            listOf(
                comping(KEYS, 0..191),
                melody(SOLOIST, 64..127),
                drums(DRUMS, 64..127),
                melody(LATECOMER, 64..127, kind = SubjectKind.Guitar),
            ),
            grid(192),
        )

        val sections = analysis.sections
        assertEquals(3, sections.size, "Expected quiet, loud, quiet, but found $sections")
        assertTrue(abs(sections[1].start - time(64)) <= time(8), "The loud section should begin near beat 64")
        assertTrue(abs(sections[2].start - time(128)) <= time(8), "The quiet ending should begin near beat 128")
        assertEquals(SectionRole.Intro, sections[0].role)
        assertEquals(SectionRole.Peak, sections[1].role)
        assertEquals(SectionRole.Outro, sections[2].role)
    }

    @Test
    fun `an instrument is on stage around its notes and through short rests`() {
        val visibility = StageVisibility.of(
            SubjectNotes(
                0,
                SubjectKind.Lead,
                listOf(
                    NoteSample(10.0, 11.0, 60, 100),
                    NoteSample(15.0, 16.0, 60, 100),
                    NoteSample(40.0, 41.0, 60, 100),
                ),
            )
        )

        assertTrue(visibility.isVisibleAt(9.5), "An instrument appears a second before it plays")
        assertTrue(visibility.isVisibleThroughout(9.5, 17.5), "A four-second rest is short enough to stay on stage")
        assertFalse(visibility.isVisibleAt(25.0), "A long rest takes the instrument off stage")
        assertFalse(visibility.isVisibleThroughout(17.0, 40.0), "The shot would outlast the instrument's time on stage")
        assertTrue(visibility.isVisibleAt(42.5), "An instrument stays two seconds after its last note")
        assertFalse(visibility.isVisibleAt(43.5))
    }

    @Test
    @Spec("camera.cinematic.ignores-echoes")
    fun `a part that only echoes another is not filmed as a second player`() {
        // The trick arrangers use for a delay effect: the same line, an octave up, a moment later.
        val lead = melody(SOLOIST, 0..63, kind = SubjectKind.Keys)
        val echo = SubjectNotes(
            ECHO,
            SubjectKind.Keys,
            lead.notes.map { it.copy(start = it.start + 0.08, end = it.end + 0.08, note = it.note + 12) },
        )
        val parts = listOf(lead, echo, comping(KEYS, 0..63), drums(DRUMS, 0..63))

        assertEquals(mapOf(ECHO to SOLOIST), Echoes.find(parts), "The copy should be found to echo the lead")
        val analysis = SongAnalysis.of(parts, grid(64))
        assertTrue(
            analysis.moments.none { ECHO in it.subjects },
            "The echo should never be featured, but the moments were ${analysis.moments}"
        )
        val echoInterest = analysis.meanInterest(ECHO, 0..63)
        val leadInterest = analysis.meanInterest(SOLOIST, 0..63)
        assertTrue(
            echoInterest < leadInterest * 0.3f,
            "The echo ($echoInterest) should be far less interesting than the line it echoes ($leadInterest)"
        )
    }

    @Test
    fun `parts that play lines of their own are not echoes`() {
        val lead = melody(SOLOIST, 0..63)
        // A harmony a third above, in step with the lead, is a second line, not an echo.
        val harmony = SubjectNotes(ECHO, SubjectKind.Lead, lead.notes.map { it.copy(note = it.note + 4) })
        assertTrue(
            Echoes.find(listOf(lead, harmony, comping(KEYS, 0..63), drums(DRUMS, 0..63))).isEmpty(),
            "Parts playing their own notes should not be taken for echoes"
        )
    }

    @Test
    fun `a choir singing the tune is as interesting as the player doubling it`() {
        val analysis = SongAnalysis.of(
            listOf(
                melody(SOLOIST, 0..63),
                melody(CHOIR, 0..63, kind = SubjectKind.Ensemble),
                comping(KEYS, 0..63),
                drums(DRUMS, 0..63),
            ),
            grid(64),
        )

        val doubled = analysis.features.getValue(SOLOIST).topVoice.indices
            .filter { analysis.features.getValue(SOLOIST).topVoice[it] }
        assertTrue(doubled.isNotEmpty(), "The tune should be carried by someone")
        assertTrue(
            doubled.all { analysis.features.getValue(CHOIR).topVoice[it] },
            "Parts playing the same notes should both be carrying the tune"
        )
        val player = analysis.meanInterest(SOLOIST, 8..55)
        val choir = analysis.meanInterest(CHOIR, 8..55)
        assertTrue(
            choir >= 0.95f * player,
            "A choir playing the tune ($choir) should be about as interesting as its double ($player), " +
                "not discounted as padding"
        )
    }

    private fun SongAnalysis.meanInterest(id: Int, beats: IntRange): Float =
        beats.map { features.getValue(id).interest[it] }.average().toFloat()

    private companion object {
        const val SOLOIST = 0
        const val PADS = 1
        const val DRUMS = 2
        const val LATECOMER = 3
        const val KEYS = 4
        const val ECHO = 5
        const val CHOIR = 6
    }
}
