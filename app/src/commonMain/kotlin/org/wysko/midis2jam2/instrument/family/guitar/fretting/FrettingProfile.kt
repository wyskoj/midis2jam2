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
 * Everything the fretting engine needs to know about one kind of instrument and how it is played.
 *
 * @property name A short label for diagnostics, such as `GUITAR (distortion)`.
 * @property stringCount The number of strings.
 * @property fretCount The highest fret (or, on fretless instruments, the highest semitone position) that can be played.
 * @property scaleLengthMm The vibrating string length, which sets how far apart frets are.
 * @property comfortableSpanMm How far the fretting hand spans without effort.
 * @property maxSpanMm The furthest the fretting hand can stretch at all.
 * @property accessFret Frets above this are awkward to reach because of the body.
 * @property bowed Whether notes are bowed, so notes sounding together must be on adjacent strings.
 * @property tunings The tunings the instrument might be in, most likely first.
 * @property capoRange The capo positions to consider; `0..0` for instruments that don't use one.
 * @property selection How the tuning and capo are chosen.
 * @property harmonics Whether natural harmonics are offered as positions (the Guitar Harmonics program).
 * @property bendStrings The strings bends are usually played on, or `null` if any string is as good.
 * @property rhythm The weights for chordal, rhythm playing.
 * @property lead The weights for single-note, lead playing.
 */
class FrettingProfile(
    val name: String,
    val stringCount: Int,
    val fretCount: Int,
    val scaleLengthMm: Double,
    val comfortableSpanMm: Double,
    val maxSpanMm: Double,
    val accessFret: Int,
    val bowed: Boolean,
    val tunings: List<Tuning>,
    val capoRange: IntRange = 0..0,
    val selection: TuningSelection = TuningSelection(),
    val harmonics: Boolean = false,
    val bendStrings: IntRange? = null,
    val rhythm: FrettingWeights,
    val lead: FrettingWeights = rhythm,
) {
    init {
        require(tunings.isNotEmpty()) { "A profile needs at least one tuning." }
        require(tunings.all { it.size == stringCount }) { "Every tuning of $name must have $stringCount strings." }
    }

    /** The fret geometry of this instrument. */
    val geometry: FretGeometry = FretGeometry(scaleLengthMm)

    /** The tuning assumed when nothing else is known. */
    val defaultTuning: Tuning get() = tunings.first()

    /** Returns a copy of this profile with different weights. */
    fun withWeights(rhythm: FrettingWeights = this.rhythm, lead: FrettingWeights = this.lead): FrettingProfile =
        FrettingProfile(
            name, stringCount, fretCount, scaleLengthMm, comfortableSpanMm, maxSpanMm, accessFret, bowed, tunings,
            capoRange, selection, harmonics, bendStrings, rhythm, lead,
        )

    /** Returns a copy of this profile that chooses its tuning and capo differently. */
    fun withSelection(selection: TuningSelection): FrettingProfile =
        FrettingProfile(
            name, stringCount, fretCount, scaleLengthMm, comfortableSpanMm, maxSpanMm, accessFret, bowed, tunings,
            capoRange, selection, harmonics, bendStrings, rhythm, lead,
        )

    override fun toString(): String = name
}

/** How a guitar part is played, which follows from its General MIDI program. */
enum class GuitarStyle(val label: String) {
    /** Nylon- and steel-string acoustic guitars. */
    ACOUSTIC("acoustic"),

    /** Clean electric guitar. */
    CLEAN("clean"),

    /** Jazz guitar. */
    JAZZ("jazz"),

    /** Palm-muted electric guitar. */
    MUTED("muted"),

    /** Overdriven or distorted electric guitar. */
    DRIVEN("driven"),

    /** Guitar harmonics. */
    HARMONICS("harmonics"),
}

/** How a bass part is played. */
enum class BassStyle(val label: String) {
    /** Fingered, picked or slapped electric bass. */
    STANDARD("standard"),

    /** Fretless bass. */
    FRETLESS("fretless"),

    /** Synth bass, usually written on a keyboard. */
    SYNTH("synth"),
}

/**
 * The profiles of every instrument the fretting engine plays.
 *
 * The weights are starting points, to be calibrated against human tablature (see `FrettingCorpusBenchmark`).
 *
 * With the guitar weights, the benchmark puts notes on the string the player used for 82.7% of GuitarSet's comping and
 * 66.6% of its solos (63.5% when the solos are written as a sequencer would, notes overlapping), and 72.9% of AnimeTAB's
 * arrangements. The other instruments have no tablature to calibrate against, so their weights are set by hand.
 */
object FrettingProfiles {
    /**
     * Rhythm weights calibrated on GuitarSet's comping (acoustic guitar, standard tuning; see
     * `FrettingCorpusBenchmark`). Values the search pushed to implausible extremes are moderated.
     */
    private val GUITAR_RHYTHM = FrettingWeights(
        position = 0.02,
        positionTarget = 4.0,
        stretch = 0.95,
        finger = 0.085,
        barre = 0.075,
        openString = -0.03,
        pastAccess = 0.6,
        innerMute = 0.9,
        shift = 0.41,
        shiftOnset = 0.16,
        stickiness = -0.3,
        cross = 0.015,
        repeat = -1.125,
        shape = -0.21,
        legato = -0.075,
        steal = 12.0,
        release = 0.85,
        openAway = 0.2,
    )

    /**
     * Lead weights calibrated on GuitarSet's solos. The search overfitted some of them (it began rewarding barres
     * and letting go of held notes); those are kept at neutral or small positive values instead, and staying in
     * position keeps a small reward.
     */
    private val GUITAR_LEAD = FrettingWeights(
        position = 0.01,
        positionTarget = 4.0,
        stretch = 0.5,
        finger = 0.02,
        barre = 0.0,
        openString = -0.005,
        pastAccess = 1.0,
        innerMute = 0.8,
        shift = 1.05,
        shiftOnset = 0.1,
        stickiness = 0.1,
        cross = 0.04,
        repeat = -2.5,
        shape = -0.3,
        legato = -0.1,
        steal = 12.0,
        release = 0.25,
        openAway = 0.05,
    )

    /** A six-string guitar played in [style]. */
    fun guitar(style: GuitarStyle): FrettingProfile {
        val (rhythm, lead) = when (style) {
            GuitarStyle.ACOUSTIC, GuitarStyle.CLEAN, GuitarStyle.MUTED, GuitarStyle.HARMONICS -> GUITAR_RHYTHM to GUITAR_LEAD
            GuitarStyle.JAZZ -> GUITAR_RHYTHM.copy(openString = 0.2, innerMute = 0.3) to GUITAR_LEAD.copy(openString = 0.1)
            GuitarStyle.DRIVEN -> GUITAR_RHYTHM.copy(openString = 0.0, shape = -1.0) to GUITAR_LEAD.copy(openString = 0.05)
        }
        return FrettingProfile(
            name = "GUITAR (${style.label})",
            stringCount = 6,
            fretCount = 22,
            scaleLengthMm = 648.0,
            comfortableSpanMm = 100.0,
            maxSpanMm = 130.0,
            accessFret = if (style == GuitarStyle.ACOUSTIC) 14 else 19,
            bowed = false,
            tunings = Tunings.GUITAR,
            capoRange = 0..7,
            selection = TuningSelection(
                capoPrior = when (style) {
                    GuitarStyle.ACOUSTIC -> 2.0
                    GuitarStyle.CLEAN -> 2.5
                    else -> 3.5
                },
            ),
            harmonics = style == GuitarStyle.HARMONICS,
            bendStrings = 3..5,
            rhythm = rhythm,
            lead = lead,
        )
    }

    /** A four-string bass guitar played in [style]. */
    fun bass(style: BassStyle): FrettingProfile {
        val weights = FrettingWeights(
            position = 0.05,
            openString = if (style == BassStyle.FRETLESS) -0.05 else -0.15,
            shift = 0.9,
            stickiness = 0.3,
            cross = 0.1,
            legato = if (style == BassStyle.FRETLESS) -0.5 else -0.3,
            innerMute = 0.3,
        )
        return FrettingProfile(
            name = "BASS (${style.label})",
            stringCount = 4,
            fretCount = 22,
            scaleLengthMm = 864.0,
            comfortableSpanMm = 100.0,
            maxSpanMm = 140.0,
            accessFret = 17,
            bowed = false,
            tunings = Tunings.BASS,
            rhythm = weights,
        )
    }

    /** A four-string tenor banjo. */
    fun banjo(): FrettingProfile = FrettingProfile(
        name = "BANJO",
        stringCount = 4,
        fretCount = 17,
        scaleLengthMm = 585.0,
        comfortableSpanMm = 100.0,
        maxSpanMm = 130.0,
        accessFret = 17,
        bowed = false,
        tunings = Tunings.BANJO,
        capoRange = 0..5,
        selection = TuningSelection(capoPrior = 2.0),
        rhythm = FrettingWeights(position = 0.08, openString = -0.2),
        lead = FrettingWeights(position = 0.04, openString = -0.1, stickiness = 0.3, legato = -0.4),
    )

    /** A three-string shamisen, which is fretless and played mostly along one string at a time. */
    fun shamisen(): FrettingProfile = FrettingProfile(
        name = "SHAMISEN",
        stringCount = 3,
        fretCount = 15,
        scaleLengthMm = 800.0,
        comfortableSpanMm = 90.0,
        maxSpanMm = 120.0,
        accessFret = 15,
        bowed = false,
        tunings = Tunings.SHAMISEN,
        rhythm = FrettingWeights(position = 0.03, openString = -0.1, shift = 0.45, cross = 0.3, legato = -0.5),
    )

    private fun bowed(name: String, scaleLengthMm: Double, comfortable: Double, max: Double, tunings: List<Tuning>) =
        FrettingProfile(
            name = name,
            stringCount = 4,
            fretCount = 24,
            scaleLengthMm = scaleLengthMm,
            comfortableSpanMm = comfortable,
            maxSpanMm = max,
            accessFret = 24,
            bowed = true,
            tunings = tunings,
            rhythm = FrettingWeights(position = 0.15, openString = -0.1, stickiness = 0.3, cross = 0.1, innerMute = 0.0),
        )

    /** A violin. */
    fun violin(): FrettingProfile = bowed("VIOLIN", 328.0, 75.0, 90.0, Tunings.VIOLIN)

    /** A fiddle, which is a violin by another name. */
    fun fiddle(): FrettingProfile = bowed("FIDDLE", 328.0, 75.0, 90.0, Tunings.VIOLIN)

    /** A viola. */
    fun viola(): FrettingProfile = bowed("VIOLA", 380.0, 85.0, 100.0, Tunings.VIOLA)

    /** A cello. */
    fun cello(): FrettingProfile = bowed("CELLO", 690.0, 100.0, 130.0, Tunings.CELLO)

    /** A double bass. */
    fun doubleBass(): FrettingProfile = bowed("DOUBLE BASS", 1060.0, 110.0, 140.0, Tunings.DOUBLE_BASS)
}
