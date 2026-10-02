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

import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Violin
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Builds the band for every General MIDI program and checks what appears on stage.
 *
 * This is the broadest test in the suite. Constructing an instrument loads its models,
 * textures and data tables, so one run exercises the assignment table, every instrument
 * constructor, and the hundreds of asset paths they name between them.
 */
class InstrumentAssignmentTest {

    @Test
    @Spec("app.instruments.all-programs-construct", "midi.assignment.by-program-change")
    fun `every General MIDI program builds the instrument it always has`() {
        val expected = goldenTable()
        val actual = mutableMapOf<Int, List<String>>()

        // No managers: this test only asks what the assignment builds, and skipping them
        // keeps a hundred and twenty-eight assignments quick.
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            MidiFixtures.GENERAL_MIDI_PROGRAMS.forEach { program ->
                actual[program] = performance
                    .assignFor(MidiFixtures.singleProgram(program))
                    .mapNotNull { it::class.simpleName }
                    .sorted()
            }
        }

        val differences = MidiFixtures.GENERAL_MIDI_PROGRAMS.mapNotNull { program ->
            val was = expected[program] ?: return@mapNotNull "program $program is missing from the golden table"
            val now = actual.getValue(program)
            if (was == now) null else "program $program: was ${was.render()}, now ${now.render()}"
        }

        if (differences.isNotEmpty()) {
            fail(
                "The instrument assignment has changed. If this was intended, update\n" +
                    "app/src/desktopTest/resources/golden/instrument-assignment.txt.\n\n" +
                    differences.joinToString("\n") { "  $it" }
            )
        }
    }

    @Test
    @Spec("midi.assignment.default-piano")
    fun `a file with no program change gets a piano`() {
        HeadlessPerformance.start(MidiFixtures.noProgramChange()).use { performance ->
            val instruments = performance.instruments

            assertEquals(1, instruments.size, "Expected exactly one instrument, got $instruments")
            assertTrue(
                instruments.single() is Keyboard,
                "The documentation promises a piano when there is no program change, " +
                    "but got ${instruments.single()::class.simpleName}"
            )
        }
    }

    @Test
    fun `the assignment table covers exactly the General MIDI programs`() {
        // MIDI defines programs 0 through 127. GS, XG and GM2 reach further with bank select,
        // not with more programs, and fall back to these.
        assertEquals(0, MidiFixtures.GENERAL_MIDI_PROGRAMS.first)
        assertEquals(127, MidiFixtures.GENERAL_MIDI_PROGRAMS.last)

        val table = goldenTable()
        assertEquals(
            MidiFixtures.GENERAL_MIDI_PROGRAMS.toList(),
            table.keys.sorted(),
            "The assignment table should cover the General MIDI programs, and only those"
        )
    }

    @Test
    @Spec("midi.assignment.same-look-merges")
    fun `switching between programs that look the same keeps one instrument`() {
        // Programs 25 and 26 (zero-based 24 and 25) are both drawn as the acoustic guitar.
        HeadlessPerformance.start(MidiFixtures.programSwitch(24, 25), attachManagers = false).use { performance ->
            val guitars = performance.instruments.filterIsInstance<Guitar>()
            assertEquals(1, guitars.size, "Expected one guitar across the program change, got ${performance.instruments}")
        }
        // Programs that look different still get an instrument each.
        HeadlessPerformance.start(MidiFixtures.programSwitch(24, 26), attachManagers = false).use { performance ->
            val guitars = performance.instruments.filterIsInstance<Guitar>()
            assertEquals(2, guitars.size, "Nylon and jazz guitars should be two instruments, got ${performance.instruments}")
        }
    }

    @Test
    fun `a note held across a program change is released by the instrument that started it`() {
        // If the note off went to the new program's instrument instead, the piano's key would never come back up.
        val file = MidiFixtures.noteHeldAcrossProgramChange(first = 0, second = 40)

        HeadlessPerformance.start(file, attachManagers = false).use { performance ->
            val piano = performance.instruments.filterIsInstance<Keyboard>().single()
            val violin = performance.instruments.filterIsInstance<Violin>().single()
            val held = MidiFixtures.HELD_NOTE.toByte()

            val pianoArcs = piano.timedArcs.filter { it.noteOn.note == held }
            assertEquals(1, pianoArcs.size, "The piano should play the held note once, got $pianoArcs")
            assertEquals(
                2 * MidiFixtures.TICKS_PER_QUARTER,
                pianoArcs.single().noteOff.tick,
                "The held note should end at its note off, after the program change",
            )
            assertTrue(
                violin.timedArcs.none { it.noteOn.note == held || it.noteOff.note == held },
                "The violin should know nothing of the piano's held note, got ${violin.timedArcs}",
            )
        }
    }

    @Test
    fun `an empty file builds no instruments`() {
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            assertEquals(emptyList(), performance.instruments)
        }
    }

    @Test
    fun `the whole band builds in one performance`() {
        HeadlessPerformance.start(MidiFixtures.theWholeBand()).use { performance ->
            val built = performance.instruments

            assertTrue(
                built.size >= MINIMUM_WHOLE_BAND_SIZE,
                "Expected the whole band to build at least $MINIMUM_WHOLE_BAND_SIZE instruments, got ${built.size}"
            )
            assertTrue(
                built.any { it::class.simpleName?.contains("DrumSet") == true },
                "The percussion channel produced no drum set: ${built.map { it::class.simpleName }.distinct()}"
            )
        }
    }

    @Test
    fun `every percussion note is animated by something`() {
        HeadlessPerformance.start(MidiFixtures.everyPercussionNote()).use { performance ->
            assertTrue(
                performance.instruments.isNotEmpty(),
                "The percussion channel produced no instruments at all"
            )
        }
    }

    @Test
    fun `the unimplemented programs are the ones we know about`() {
        // The FAQ admits some instruments are missing. This pins down which, so that a program
        // cannot quietly stop being animated without anyone noticing.
        val silent = goldenTable().filterValues { it.isEmpty() }.keys.sorted()

        assertEquals(
            KNOWN_UNIMPLEMENTED_PROGRAMS,
            silent,
            "The set of General MIDI programs with no instrument has changed"
        )
    }

    private companion object {

        /**
         * General MIDI programs with no instrument yet: English Horn, Bassoon, Shakuhachi,
         * Sitar, Koto, Shanai and Seashore.
         */
        val KNOWN_UNIMPLEMENTED_PROGRAMS = listOf(69, 70, 77, 104, 107, 111, 122)

        /** A floor, not an exact count, so that adding an instrument does not fail the test. */
        const val MINIMUM_WHOLE_BAND_SIZE = 100

        const val GOLDEN = "/golden/instrument-assignment.txt"

        fun List<String>.render(): String = if (isEmpty()) "nothing" else joinToString(", ")

        fun goldenTable(): Map<Int, List<String>> {
            val text = checkNotNull(InstrumentAssignmentTest::class.java.getResourceAsStream(GOLDEN)) {
                "The golden assignment table is missing from the test classpath at $GOLDEN"
            }.bufferedReader().use { it.readText() }

            return text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .associate { line ->
                    val program = line.substringBefore('=').toInt()
                    val instruments = line.substringAfter('=')
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .sorted()
                    program to instruments
                }
        }
    }
}
