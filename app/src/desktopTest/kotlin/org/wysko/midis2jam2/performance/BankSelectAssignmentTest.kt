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

import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.assignment.Looks
import org.wysko.midis2jam2.instrument.algorithmic.assignment.VoiceCatalogue
import org.wysko.midis2jam2.instrument.algorithmic.assignment.VoiceCatalogues
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.Flute
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.MidiFixtures.Patch
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Choosing instruments by bank select as well as program, the way GS, XG and GM2 files expect.
 *
 * These specifications have hundreds of voices beyond General MIDI's 128, picked with bank select. A voice with an
 * instrument of its own should appear as that instrument, and every other voice as its General MIDI counterpart,
 * which is what the specifications themselves fall back to.
 */
class BankSelectAssignmentTest {

    @Test
    @Spec("midi.assignment.bank-select.gm2")
    fun `a GM2 variation with an instrument of its own appears as that instrument`() {
        // Program 121 is Guitar Fret Noise, drawn as a guitar; its variation "Acoustic Bass String Slap" is a bass.
        val instruments = build(MidiFixtures.bankSelect(MidiFixtures.GM2_SYSTEM_ON, Patch(121, 2, 120)))

        assertTrue(instruments.any { it is AcousticBass }, "Acoustic Bass String Slap should be a bass: $instruments")
        assertTrue(instruments.none { it is Guitar }, "It shouldn't fall back to the guitar: $instruments")
    }

    @Test
    @Spec("midi.assignment.bank-select.fallback")
    fun `a GM2 variation without an instrument of its own appears as its General MIDI voice`() {
        // Ukulele is a variation of the nylon guitar.
        val instruments = build(MidiFixtures.bankSelect(MidiFixtures.GM2_SYSTEM_ON, Patch(121, 1, 24)))

        assertEquals(listOf("Guitar"), instruments.map { it::class.simpleName }, "Ukulele should fall back to guitar")
    }

    @Test
    @Spec("midi.assignment.mode.reset-message")
    fun `without a GM2 reset, the same banks are ignored`() {
        val instruments = build(MidiFixtures.bankSelect(null, Patch(121, 2, 120)))

        assertTrue(instruments.any { it is Guitar }, "In General MIDI, program 121 is a guitar: $instruments")
        assertTrue(instruments.none { it is AcousticBass }, "Bank select should be ignored: $instruments")
    }

    @Test
    @Spec("midi.assignment.bank-select.gs", "midi.assignment.same-look-merges")
    fun `stepping through GS piano variations stays on one keyboard`() {
        // Piano 1, then its variation Upright P w (bank 8), then Mild Piano (bank 2).
        val file = MidiFixtures.bankSelect(MidiFixtures.GS_RESET, Patch(0, 0, 0), Patch(8, 0, 0), Patch(2, 0, 0))
        val instruments = build(file)

        assertEquals(1, instruments.filterIsInstance<Keyboard>().size, "Expected one keyboard, got $instruments")
    }

    @Test
    @Spec("midi.assignment.bank-select.xg")
    fun `an XG variation without an instrument of its own appears as its General MIDI voice`() {
        // GrandPiano KSP is bank 0/1 of program 1.
        val file = MidiFixtures.bankSelect(MidiFixtures.XG_SYSTEM_ON, Patch(0, 1, 0))
        val instruments = build(file)

        assertEquals(listOf("Keyboard"), instruments.map { it::class.simpleName })
    }

    @Test
    @Spec("midi.assignment.mode.settings")
    fun `a file without a reset is read in the mode the app resets the synthesizer to`() {
        // In XG, bank 127 is a drum kit, even off channel 10. In General MIDI, bank select means nothing.
        val file = MidiFixtures.bankSelect(null, Patch(127, 0, 0), firstNote = 36)
        val xg = AppSettings().apply {
            playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage = true
            playbackSettings.midiSpecificationResetSettings.midiSpecification = MidiSpecification.ExtendedGeneral
        }

        HeadlessPerformance.start(file, settings = xg).use { performance ->
            assertTrue(
                performance.instruments.any { it is DrumSet },
                "With the XG reset on, bank 127 is drums, but got ${performance.instruments}",
            )
        }
        HeadlessPerformance.start(file).use { performance ->
            assertTrue(
                performance.instruments.any { it is Keyboard } && performance.instruments.none { it is DrumSet },
                "With no reset, bank 127 is ignored and program 1 is a piano, but got ${performance.instruments}",
            )
        }
    }

    @Test
    fun `a flute key click is a flute in GM2`() {
        val instruments = build(MidiFixtures.bankSelect(MidiFixtures.GM2_SYSTEM_ON, Patch(121, 1, 121)))

        assertTrue(instruments.any { it is Flute }, "Flute Key Click should be drawn as a flute: $instruments")
    }

    @Test
    @Spec("app.instruments.all-programs-construct")
    fun `every look a catalogue names builds an instrument`() {
        val problems = mutableListOf<String>()

        HeadlessPerformance.start(MidiFixtures.empty()).use { performance ->
            melodic.forEach { (reset, catalogue) ->
                catalogue.entries.filter { it.look != null && it.look != Looks.NONE }.forEach { voice ->
                    val file = MidiFixtures.bankSelect(reset, Patch(voice.msb, voice.lsb, voice.program - 1))
                    val built = runCatching { performance.assignFor(file) }
                    if (built.getOrNull().isNullOrEmpty()) problems += "${voice.name} (${voice.look}): $built"
                }
            }
            kits.forEach { (reset, catalogue) ->
                catalogue.entries.filter { it.look != null && it.look != Looks.NONE }.forEach { kit ->
                    val file = MidiFixtures.bankSelect(
                        reset,
                        Patch(kit.msb, kit.lsb, kit.program - 1),
                        channel = MidiFixtures.PERCUSSION_CHANNEL,
                        // Notes 39-42 are drawn by every layout: the SFX kit's first sounds start at 39.
                        firstNote = 39,
                    )
                    val built = runCatching { performance.assignFor(file) }
                    if (built.getOrNull().isNullOrEmpty()) problems += "${kit.name} (${kit.look}): $built"
                }
            }
        }

        if (problems.isNotEmpty()) fail("These catalogue entries built nothing:\n" + problems.joinToString("\n"))
    }

    private fun build(file: org.wysko.kmidi.midi.TimeBasedSequence): List<Instrument> =
        HeadlessPerformance.start(file, attachManagers = false).use { it.instruments }

    private companion object {
        val melodic: List<Pair<ByteArray?, VoiceCatalogue>> = listOf(
            null to VoiceCatalogues.gm,
            MidiFixtures.GS_RESET to VoiceCatalogues.gs,
            MidiFixtures.XG_SYSTEM_ON to VoiceCatalogues.xg,
            MidiFixtures.GM2_SYSTEM_ON to VoiceCatalogues.gm2,
        )

        val kits: List<Pair<ByteArray?, VoiceCatalogue>> = listOf(
            null to VoiceCatalogues.gmKits,
            MidiFixtures.GS_RESET to VoiceCatalogues.gsKits,
            MidiFixtures.XG_SYSTEM_ON to VoiceCatalogues.xgKits,
            MidiFixtures.GM2_SYSTEM_ON to VoiceCatalogues.gm2Kits,
        )
    }
}
