/*
 * Copyright (C) 2021 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.algorithmic

/** Handles the lookup of fingerings for MIDI notes. */
interface FingeringManager<E> {
    /**
     * Given a MIDI note, returns the fingering associated with that note, or null if the note is outside the
     * instrument's defined range.
     *
     * @param midiNote the MIDI note
     */
    fun fingering(midiNote: Int): E?
}