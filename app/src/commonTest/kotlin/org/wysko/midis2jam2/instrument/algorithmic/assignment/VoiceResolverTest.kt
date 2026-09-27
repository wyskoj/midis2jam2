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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState.Melody
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState.Rhythm
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Choosing how a channel's voice appears, from the specification's catalogue or the General MIDI one.
 *
 * GS, XG and GM2 have hundreds of voices the app has no instrument for. Each should still appear as its General MIDI
 * counterpart, as the specifications themselves fall back, and never as nothing or as something unrelated.
 */
class VoiceResolverTest {

    private val resolver = VoiceResolver(
        object : VoiceResolver.Catalogues {
            override val gm = catalogue(
                """
                - { program: 1, name: "Acoustic Grand Piano", look: Keyboard.Piano }
                - { program: 29, name: "Electric Guitar (muted)", look: Guitar.Muted }
                """,
            )
            override val gs = catalogue(
                """
                - { program: 1, msb: 8, name: "Piano 1w" }
                - { program: 1, msb: 24, name: "Piano + Str.", look: None }
                """,
            )
            override val xg = catalogue("""- { program: 29, msb: 0, lsb: 3, name: "Jazz Man", look: Guitar.Jazz }""")
            override val gm2 = catalogue("""- { program: 29, msb: 121, lsb: 3, name: "Jazz Man", look: Guitar.Jazz }""")
            override val gmKits = catalogue("""- { program: 9, name: "Room", look: Room }""")
            override val gsKits = catalogue("""- { program: 49, name: "Orchestra", look: Orchestra }""")
            override val xgKits = catalogue("""- { program: 1, msb: 126, name: "SFX Kit 1", look: None }""")
            override val gm2Kits = catalogue("""- { program: 33, msb: 120, name: "Jazz Set", look: Jazz }""")
        },
    )

    @Test
    @Spec("midi.assignment.bank-select.xg", "midi.assignment.bank-select.gm2")
    fun `a voice with a look of its own appears as that look`() {
        assertEquals("Guitar.Jazz", resolver.melodic(setup(MidiMode.GM2, msb = 121, lsb = 3, program = 28))?.id)
        assertEquals("Guitar.Jazz", resolver.melodic(setup(MidiMode.XG, msb = 0, lsb = 3, program = 28))?.id)
    }

    @Test
    @Spec("midi.assignment.bank-select.fallback")
    fun `a voice without a look of its own appears as its General MIDI program`() {
        assertEquals("Keyboard.Piano", resolver.melodic(setup(MidiMode.GS, msb = 8, program = 0))?.id, "Listed, no look")
        assertEquals("Guitar.Muted", resolver.melodic(setup(MidiMode.GM2, msb = 121, lsb = 1, program = 28))?.id, "Unlisted")
    }

    @Test
    fun `a voice marked None puts nothing on stage`() {
        assertEquals(Looks.NONE, resolver.melodic(setup(MidiMode.GS, msb = 24, program = 0))?.id)
    }

    @Test
    fun `General MIDI ignores bank select`() {
        assertEquals("Guitar.Muted", resolver.melodic(setup(MidiMode.GM, msb = 121, lsb = 3, program = 28))?.id)
    }

    @Test
    fun `GS ignores the LSB, which only picks the sound map`() {
        assertEquals(Looks.NONE, resolver.melodic(setup(MidiMode.GS, msb = 24, lsb = 3, program = 0))?.id)
    }

    @Test
    fun `kits come from the specification's kit catalogue, then the General MIDI one, then Standard`() {
        assertEquals("Jazz", resolver.kit(setup(MidiMode.GM2, msb = 120, program = 32, state = Rhythm)).id)
        assertEquals("Orchestra", resolver.kit(setup(MidiMode.GS, program = 48, state = Rhythm)).id)
        assertEquals(Looks.NONE, resolver.kit(setup(MidiMode.XG, msb = 126, program = 0, state = Rhythm)).id)
        assertEquals("Room", resolver.kit(setup(MidiMode.XG, msb = 127, program = 8, state = Rhythm)).id)
        assertEquals("Standard", resolver.kit(setup(MidiMode.GM, program = 100, state = Rhythm)).id)
    }

    private fun setup(mode: MidiMode, msb: Int = 0, lsb: Int = 0, program: Int, state: ChannelState = Melody) =
        ChannelSetup(mode, state, msb, lsb, program)

    private fun catalogue(yaml: String) = VoiceCatalogue.parse(yaml.trimIndent())
}
