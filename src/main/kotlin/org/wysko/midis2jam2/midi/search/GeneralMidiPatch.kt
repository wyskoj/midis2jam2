/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.midi.search

import java.io.IOException

/**
 * An instance of a "patch" (i.e., instrument) as defined in the General MIDI specification.
 */
class GeneralMidiPatch(
    /** The MIDI value associated with this patch. */
    val value: Int,
    private val name: String
) {
    override fun toString(): String = name

    companion object {
        /**
         * Loads and returns the list of General MIDI patches from a resource file.
         */
        fun loadList(): Collection<GeneralMidiPatch> {
            Companion::class.java.getResource("/instruments.txt")
                ?.let { return it.readText().split("\n").mapIndexed { index, s -> GeneralMidiPatch(index, s) } }
                ?: throw IOException("The patch list could not be loaded.")
        }
    }
}
