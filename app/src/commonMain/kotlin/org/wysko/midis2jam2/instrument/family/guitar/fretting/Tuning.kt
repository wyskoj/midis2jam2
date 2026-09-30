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
 * The open-string pitches of a fretted or bowed instrument.
 *
 * @property id A stable identifier, such as `drop-d`.
 * @property displayName A short human-readable name, such as `Drop D`.
 * @property openStrings The MIDI note of each open string, from the lowest (thickest) string up.
 * @property prior How unlikely this tuning is before looking at the music. The default tuning is `0`; the
 * [TuningSelector] only picks another when the music is clearly easier to play in it.
 */
class Tuning(
    val id: String,
    val displayName: String,
    openStrings: IntArray,
    val prior: Double = 0.0,
) {
    private val strings = openStrings.copyOf()

    /** The MIDI note of each open string, from the lowest (thickest) string up. */
    val openStrings: IntArray get() = strings.copyOf()

    /** The number of strings. */
    val size: Int get() = strings.size

    /** The open pitch of [string]. */
    operator fun get(string: Int): Int = strings[string]

    /** The lowest open-string pitch. */
    val lowest: Int get() = strings.min()

    /** The open strings as note names, e.g. `D2 A2 D3 G3 B3 E4`. */
    val noteNames: String get() = strings.joinToString(" ") { noteName(it) }

    override fun equals(other: Any?): Boolean = other is Tuning && other.id == id && other.strings.contentEquals(strings)

    override fun hashCode(): Int = id.hashCode() * 31 + strings.contentHashCode()

    override fun toString(): String = "$displayName [$noteNames]"
}

private val NOTE_NAMES = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B")

/** Names a MIDI note, with middle C (60) as `C4`. */
fun noteName(midiNote: Int): String = NOTE_NAMES[midiNote.mod(12)] + (midiNote.floorDiv(12) - 1)

/**
 * The tunings each instrument might be in, most likely first.
 *
 * Only common tunings with the instrument's usual number of strings are listed; the models always show six
 * strings on a guitar, four on a bass or banjo, and three on a shamisen.
 */
object Tunings {
    private fun t(id: String, name: String, prior: Double, vararg strings: Int) = Tuning(id, name, strings, prior)

    /** Six-string guitar tunings. */
    val GUITAR: List<Tuning> = listOf(
        t("e-standard", "E std", 0.0, 40, 45, 50, 55, 59, 64),
        t("eb-standard", "Eb std", 3.0, 39, 44, 49, 54, 58, 63),
        t("drop-d", "Drop D", 2.0, 38, 45, 50, 55, 59, 64),
        t("d-standard", "D std", 3.5, 38, 43, 48, 53, 57, 62),
        t("drop-c-sharp", "Drop C#", 3.5, 37, 44, 49, 54, 58, 63),
        t("drop-c", "Drop C", 4.0, 36, 43, 48, 53, 57, 62),
        t("c-standard", "C std", 5.0, 36, 41, 46, 51, 55, 60),
        t("drop-b", "Drop B", 5.0, 35, 42, 47, 52, 56, 61),
        t("dadgad", "DADGAD", 5.0, 38, 45, 50, 55, 57, 62),
        t("open-g", "Open G", 5.0, 38, 43, 50, 55, 59, 62),
        t("open-d", "Open D", 5.0, 38, 45, 50, 54, 57, 62),
        t("open-e", "Open E", 5.5, 40, 47, 52, 56, 59, 64),
        t("open-c", "Open C", 6.0, 36, 43, 48, 55, 60, 64),
    )

    /** Four-string bass tunings. */
    val BASS: List<Tuning> = listOf(
        t("e-standard", "E std", 0.0, 28, 33, 38, 43),
        t("eb-standard", "Eb std", 3.0, 27, 32, 37, 42),
        t("drop-d", "Drop D", 2.0, 26, 33, 38, 43),
        t("d-standard", "D std", 3.5, 26, 31, 36, 41),
        t("drop-c", "Drop C", 4.0, 24, 31, 36, 41),
        t("bead", "BEAD", 4.0, 23, 28, 33, 38),
    )

    /** Four-string (tenor) banjo tunings. */
    val BANJO: List<Tuning> = listOf(
        t("cgda", "CGDA", 0.0, 48, 55, 62, 69),
        t("gdae", "GDAE", 2.5, 43, 50, 57, 64),
        t("cgbd", "CGBD", 3.0, 48, 55, 59, 62),
    )

    /** Shamisen tunings (honchoshi, niagari and sansagari), on the same base pitch. */
    val SHAMISEN: List<Tuning> = listOf(
        t("niagari", "Niagari", 0.0, 50, 57, 62),
        t("honchoshi", "Honchoshi", 1.0, 50, 55, 62),
        t("sansagari", "Sansagari", 1.0, 50, 55, 60),
    )

    /** The violin's only tuning. */
    val VIOLIN: List<Tuning> = listOf(t("gdae", "GDAE", 0.0, 55, 62, 69, 76))

    /** The viola's only tuning. */
    val VIOLA: List<Tuning> = listOf(t("cgda", "CGDA", 0.0, 48, 55, 62, 69))

    /** The cello's only tuning. */
    val CELLO: List<Tuning> = listOf(t("cgda", "CGDA", 0.0, 36, 43, 50, 57))

    /** The double bass's only tuning. */
    val DOUBLE_BASS: List<Tuning> = listOf(t("eadg", "EADG", 0.0, 28, 33, 38, 43))
}
