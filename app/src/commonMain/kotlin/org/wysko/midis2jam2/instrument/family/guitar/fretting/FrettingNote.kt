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

package org.wysko.midis2jam2.instrument.family.guitar.fretting

/**
 * One note for the fretting engine to place, independent of how the MIDI file expressed it.
 *
 * @property pitch The MIDI note number.
 * @property start When the note starts, in seconds.
 * @property end When the note ends, in seconds.
 * @property velocity The MIDI velocity.
 * @property bendUp The largest upward pitch bend while the note sounds, in semitones (never negative).
 * @property bendDown The largest downward pitch bend while the note sounds, in semitones (never negative).
 */
data class FrettingNote(
    val pitch: Int,
    val start: Double,
    val end: Double,
    val velocity: Int = 100,
    val bendUp: Double = 0.0,
    val bendDown: Double = 0.0,
) {
    /** The largest bend in either direction. */
    val bend: Double get() = maxOf(bendUp, bendDown)
}
