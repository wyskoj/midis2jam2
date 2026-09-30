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
 * How much each consideration costs when choosing a fingering. Negative values are rewards.
 *
 * Costs are in loose "effort" units, where 1.0 is roughly moving the hand one fret in a hurry. A profile
 * has one set for rhythm playing and one for lead playing, blended per slice by how lead-like the music is.
 *
 * Static costs (per fingering):
 * @property position Per fret the hand sits away from [positionTarget].
 * @property positionTarget The fret (counted from the nut or capo) the hand is most at home over.
 * @property stretch Times the square of each 10 mm the hand spans beyond its comfortable reach.
 * @property finger Per finger used.
 * @property barre For a full barre; narrower barres cost proportionally less.
 * @property openString Per open string played.
 * @property pastAccess Per fret beyond where the body makes the neck awkward to reach.
 * @property innerMute Per unplayed string between played strings of a strummed chord (three notes or more).
 * @property bendImpossible A real bend that cannot happen here: on an open string, or off the end of the neck.
 * @property bendLowString A real bend on a string other than the ones bends are usually played on.
 * @property vibratoOpen A slight bend (vibrato) on an open string.
 * @property drop Per note left out because it cannot be fingered.
 * @property harmonic Per note played as a natural harmonic (only offered on instruments that play harmonics).
 *
 * Transition costs (between consecutive fingerings):
 * @property shift Per fret the hand moves, scaled by how rushed the move is.
 * @property shiftOnset For moving the hand at all.
 * @property stickiness For moving the hand in the middle of a phrase, rather than between phrases.
 * @property cross Per string the picking hand crosses between single notes, scaled by how rushed it is.
 * @property repeat For playing a repeated pitch in the same place again.
 * @property shape For a chord shape moved as a unit along the same strings.
 * @property legato For keeping a hammer-on, pull-off, slide or trill on one string.
 * @property steal Per second of ring cut off when a new note takes a string that is still sounding.
 * @property release Per second of ring cut off when the hand moves away from a held fretted note.
 * @property riff For playing a repeated passage the same way as its other occurrences.
 */
data class FrettingWeights(
    val position: Double = 0.05,
    val positionTarget: Double = 0.0,
    val stretch: Double = 1.0,
    val finger: Double = 0.1,
    val barre: Double = 0.6,
    val openString: Double = -0.1,
    val pastAccess: Double = 0.3,
    val innerMute: Double = 0.8,
    val bendImpossible: Double = 10.0,
    val bendLowString: Double = 0.5,
    val vibratoOpen: Double = 1.0,
    val drop: Double = 5.0,
    val harmonic: Double = -1.0,
    val shift: Double = 0.7,
    val shiftOnset: Double = 0.3,
    val stickiness: Double = 0.0,
    val cross: Double = 0.15,
    val repeat: Double = -0.5,
    val shape: Double = -0.4,
    val legato: Double = -0.3,
    val steal: Double = 3.0,
    val release: Double = 1.5,
    val riff: Double = -1.5,
) {
    /** The value of each weight, in declaration order. */
    fun toArray(): DoubleArray = doubleArrayOf(
        position, positionTarget, stretch, finger, barre, openString, pastAccess, innerMute, bendImpossible, bendLowString,
        vibratoOpen, drop, harmonic, shift, shiftOnset, stickiness, cross, repeat, shape, legato, steal, release,
        riff,
    )

    companion object {
        /** The names of the weights, in the order of [toArray]. */
        val NAMES: List<String> = listOf(
            "position", "positionTarget", "stretch", "finger", "barre", "openString", "pastAccess", "innerMute", "bendImpossible",
            "bendLowString", "vibratoOpen", "drop", "harmonic", "shift", "shiftOnset", "stickiness", "cross",
            "repeat", "shape", "legato", "steal", "release", "riff",
        )

        /** Builds weights from values in the order of [toArray]. */
        fun fromArray(v: DoubleArray): FrettingWeights {
            require(v.size == NAMES.size) { "Expected ${NAMES.size} weights, got ${v.size}" }
            return FrettingWeights(
                v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12], v[13], v[14],
                v[15], v[16], v[17], v[18], v[19], v[20], v[21], v[22],
            )
        }

        /** Blends [rhythm] and [lead] weights: `0` is all rhythm, `1` is all lead. */
        fun blend(rhythm: FrettingWeights, lead: FrettingWeights, leadness: Double): FrettingWeights {
            if (leadness <= 0.0 || rhythm == lead) return rhythm
            if (leadness >= 1.0) return lead
            val r = rhythm.toArray()
            val l = lead.toArray()
            return fromArray(DoubleArray(r.size) { r[it] + (l[it] - r[it]) * leadness })
        }
    }
}
