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

/**
 * Decides how a channel's current voice appears on stage, by looking it up in the voice catalogues.
 *
 * A voice the specification's catalogue gives a look appears as that look. Every other voice appears as the General
 * MIDI voice with the same program, which is what the specifications themselves fall back to: GM2 plays bank 79H/00
 * for an undefined variation, and GS plays the capital tone.
 */
class VoiceResolver(private val catalogues: Catalogues = VoiceCatalogues) {

    /** The catalogues a resolver reads. */
    interface Catalogues {
        val gm: VoiceCatalogue
        val gs: VoiceCatalogue
        val xg: VoiceCatalogue
        val gm2: VoiceCatalogue
        val gmKits: VoiceCatalogue
        val gsKits: VoiceCatalogue
        val xgKits: VoiceCatalogue
        val gm2Kits: VoiceCatalogue
    }

    /** The look for a melodic channel set up as [setup], or `null` if it has no instrument. */
    fun melodic(setup: ChannelSetup): Look? {
        val program = setup.program ?: 0
        val entry = when (setup.mode) {
            MidiMode.GM -> null
            // In GS, the LSB only picks which Sound Canvas's sound map to use; the instrument is the same.
            MidiMode.GS -> catalogues.gs[setup.msb, 0, program]
            MidiMode.XG -> catalogues.xg[setup.msb, setup.lsb, program]
                // Bank 64 is XG's sound effects, not variations of the General MIDI program with the same number.
                ?: if (setup.msb == XG_SFX_BANK) return look(Looks.NONE) else null

            MidiMode.GM2 -> catalogues.gm2[setup.msb, setup.lsb, program]
        }
        return entry?.look?.let(::look) ?: melodic(program)
    }

    /** The look for the zero-based General MIDI [program], or `null` if it has no instrument. */
    fun melodic(program: Int): Look? = catalogues.gm[0, 0, program]?.look?.let(::look)

    /** The kit look for a rhythm channel set up as [setup]. */
    fun kit(setup: ChannelSetup): KitLook {
        val program = setup.program ?: 0
        val entry = when (setup.mode) {
            MidiMode.GM -> null
            MidiMode.GS -> catalogues.gsKits[0, 0, program]
            // XG keeps its drum kits in bank 127 and SFX kits in 126. A rhythm channel on any other bank (such as
            // channel 10 left on bank 0) still plays the drum kit.
            MidiMode.XG -> catalogues.xgKits[if (setup.msb == XG_SFX_KIT_BANK) XG_SFX_KIT_BANK else XG_DRUM_BANK, 0, program]
            MidiMode.GM2 -> catalogues.gm2Kits[setup.msb, setup.lsb, program]
        }
        return entry?.look?.let(::kitLook) ?: kit(program)
    }

    /** The kit look for the zero-based kit [program]. Kits the catalogue doesn't list play the Standard kit. */
    fun kit(program: Int): KitLook = catalogues.gmKits[0, 0, program]?.look?.let(::kitLook) ?: KitLooks.Standard

    private fun look(id: String): Look = Looks[id] ?: error("The voice catalogues name an unknown look: $id")

    private companion object {
        const val XG_SFX_BANK = 64
        const val XG_SFX_KIT_BANK = 126
        const val XG_DRUM_BANK = 127
    }

    private fun kitLook(id: String): KitLook = KitLooks[id] ?: error("The kit catalogues name an unknown kit look: $id")
}
