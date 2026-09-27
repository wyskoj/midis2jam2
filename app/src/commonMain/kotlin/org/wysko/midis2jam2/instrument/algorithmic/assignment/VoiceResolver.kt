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
 */
object VoiceResolver {

    /** The look for the zero-based melodic [program], or `null` if it has no instrument. */
    fun melodic(program: Int): Look? = VoiceCatalogues.gm[0, 0, program]?.look?.let(::look)

    /** The kit look for the zero-based kit [program]. Kits the catalogue doesn't list play the Standard kit. */
    fun kit(program: Int): KitLook = VoiceCatalogues.gmKits[0, 0, program]?.look?.let(::kitLook) ?: KitLooks.Standard

    private fun look(id: String): Look = Looks[id] ?: error("The voice catalogues name an unknown look: $id")

    private fun kitLook(id: String): KitLook = KitLooks[id] ?: error("The kit catalogues name an unknown kit look: $id")
}
