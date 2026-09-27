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

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.OrchestraDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.TypicalDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.TypicalDrumShell
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.MidiFixtures.PRIMARY_RHYTHM_NOTE
import org.wysko.midis2jam2.testing.MidiFixtures.SECOND_RHYTHM_NOTE
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Channels other than channel 10 that a file turns into rhythm channels.
 *
 * GS, XG and GM2 files can make any channel a rhythm channel, and the synthesizer then plays it as drums. Before this
 * was supported, the channel's kit number was read as a melodic program: `take5.mid` sets channel 11 to the Jazz kit
 * (program 32), and its snare hits were drawn on an acoustic bass.
 */
class RhythmChannelAssignmentTest {

    private val jazz = 32
    private val standard = 0
    private val room = 8
    private val orchestra = 48

    @Test
    @Spec("midi.assignment.rhythm-channel.gs", "midi.assignment.rhythm-channel.shared-kit")
    fun `a second rhythm channel on the same kit plays on channel 10's drum set`() {
        build(MidiFixtures.secondRhythmChannel(primaryKit = jazz, secondKit = jazz)) { instruments, hits ->
            assertTrue(
                instruments.none { it is AcousticBass },
                "Channel 11 is a rhythm part playing the Jazz kit, but it was drawn as an acoustic bass",
            )

            val drumSet = instruments.filterIsInstance<DrumSet>().singleOrNull()
            assertTrue(
                drumSet is TypicalDrumSet && drumSet.shellStyle == TypicalDrumShell.Jazz,
                "Expected one Jazz drum set, got ${instruments.filterIsInstance<DrumSet>()}",
            )
            assertEquals(8, hits(drumSet).count { it == PRIMARY_RHYTHM_NOTE }, "Channel 10's bass drum hits")
            assertEquals(8, hits(drumSet).count { it == SECOND_RHYTHM_NOTE }, "Channel 11's snare hits")
        }
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.shared-kit")
    fun `a second rhythm channel on a kit with the same layout plays on channel 10's kit`() {
        build(MidiFixtures.secondRhythmChannel(primaryKit = standard, secondKit = room)) { instruments, hits ->
            val drumSets = instruments.filterIsInstance<DrumSet>()
            assertEquals(1, drumSets.size, "Only one drum set can be on stage at once, but got $drumSets")

            val drumSet = drumSets.single()
            assertTrue(
                drumSet is TypicalDrumSet && drumSet.shellStyle == TypicalDrumShell.Standard,
                "The drum set should be channel 10's Standard kit, but got $drumSet",
            )
            assertEquals(8, hits(drumSet).count { it == SECOND_RHYTHM_NOTE }, "Channel 11's snare hits")
        }
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.shared-kit")
    fun `a second rhythm channel on the orchestra kit gets a drum set of its own`() {
        build(MidiFixtures.secondRhythmChannel(primaryKit = standard, secondKit = orchestra)) { instruments, _ ->
            val drumSets = instruments.filterIsInstance<DrumSet>()

            assertEquals(2, drumSets.size, "The orchestra kit lays out its notes differently, got $drumSets")
            assertTrue(drumSets.any { it is TypicalDrumSet }, "Channel 10's Standard kit is missing: $drumSets")
            assertTrue(drumSets.any { it is OrchestraDrumSet }, "Channel 11's orchestra kit is missing: $drumSets")
        }
    }

    @Test
    fun `a second rhythm channel keeps its own kit when channel 10 is not playing`() {
        build(MidiFixtures.secondRhythmChannel(primaryKit = null, secondKit = jazz)) { instruments, _ ->
            val drumSet = instruments.filterIsInstance<DrumSet>().singleOrNull()

            assertTrue(
                drumSet is TypicalDrumSet && drumSet.shellStyle == TypicalDrumShell.Jazz,
                "An idle channel 10 shouldn't turn channel 11's Jazz kit into a Standard one, got $drumSet",
            )
        }
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.gs")
    fun `a channel turned back into a melodic part plays its instrument again`() {
        val file = MidiFixtures.secondRhythmChannel(primaryKit = jazz, secondKit = jazz, melodicFrom = 12)
        build(file) { instruments, hits ->
            val drumSet = instruments.filterIsInstance<DrumSet>().single()

            assertEquals(8, hits(drumSet).count { it == SECOND_RHYTHM_NOTE }, "Channel 11's snare hits")
            assertTrue(
                instruments.any { it is AcousticBass },
                "Channel 11 went back to being a melodic part on program 32, which should be an acoustic bass",
            )
        }
    }

    @Test
    fun `without the rhythm part message the channel is melodic, as before`() {
        val file = MidiFixtures.secondRhythmChannel(primaryKit = jazz, secondKit = jazz, asRhythmPart = false)
        build(file) { instruments, hits ->
            assertTrue(instruments.any { it is AcousticBass }, "Channel 11 is melodic, so program 32 is a bass")
            val drumSet = instruments.filterIsInstance<DrumSet>().single()
            assertEquals(0, hits(drumSet).count { it == SECOND_RHYTHM_NOTE }, "Channel 11's notes aren't drums")
        }
    }

    /**
     * Boots [file] and hands [check] its instruments, along with a way to list the notes a drum set was given.
     */
    private fun build(
        file: TimeBasedSequence,
        check: (instruments: List<Instrument>, hits: (DrumSet) -> List<Int>) -> Unit,
    ) {
        HeadlessPerformance.start(file).use { performance ->
            val hits = { drumSet: DrumSet ->
                performance.onEngineThread {
                    with(drumSet.collectorForVisibility) {
                        seek(Duration.ZERO)
                        advanceCollectAll(1.hours).map { it.note.toInt() }
                    }
                }
            }
            check(performance.instruments, hits)
        }
    }
}
