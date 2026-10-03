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

import kotlin.math.max

/**
 * One consideration in the cost of a fingering, named for the live readout.
 *
 * @property label The short name shown in the readout.
 * @property isStatic Whether the term belongs to the fingering itself, rather than to the move from the previous one.
 */
enum class CostTerm(val label: String, val isStatic: Boolean) {
    POSITION("pos", true),
    STRETCH("str", true),
    FINGERS("fing", true),
    BARRE("barre", true),
    OPEN("open", true),
    ACCESS("acc", true),
    INNER_MUTE("mute", true),
    BEND("bend", true),
    DROP("drop", true),
    HARMONIC("harm", true),
    RIFF("riff", true),
    SHIFT("shift", false),
    STICKINESS("stick", false),
    CROSS("cross", false),
    REPEAT("rep", false),
    SHAPE("shape", false),
    LEGATO("leg", false),
    STEAL("steal", false),
    RELEASE("rel", false),
    OPEN_AWAY("openup", false),
}

/** The cost of a fingering, term by term. Only built for the chosen path, to explain it. */
class CostBreakdown {
    private val values = DoubleArray(CostTerm.entries.size)

    /** Adds [value] to [term]. */
    fun add(term: CostTerm, value: Double) {
        values[term.ordinal] += value
    }

    /** The total of [term]. */
    operator fun get(term: CostTerm): Double = values[term.ordinal]

    /** The total of the static terms. */
    val staticTotal: Double get() = CostTerm.entries.filter { it.isStatic }.sumOf { values[it.ordinal] }

    /** The total of the transition terms. */
    val transitionTotal: Double get() = CostTerm.entries.filter { !it.isStatic }.sumOf { values[it.ordinal] }

    /** The terms that contributed anything, in declaration order. */
    fun nonZero(static: Boolean): List<Pair<CostTerm, Double>> =
        CostTerm.entries.filter { it.isStatic == static && values[it.ordinal] != 0.0 }.map { it to values[it.ordinal] }
}

/** The per-fingering costs, and the helpers shared with the decoder's transition costs. */
object CostModel {
    /** A bend of at least this many semitones is a real bend, rather than vibrato. */
    const val REAL_BEND: Double = 0.5

    private const val VIBRATO = 0.05
    private const val OCTAVE = 12
    private const val DYAD_SKIP_FACTOR = 0.5
    private const val URGENCY_REFERENCE = 0.25
    private const val URGENCY_MIN = 0.2
    private const val URGENCY_MAX = 3.0

    /**
     * How rushed a move is, given [available] seconds to make it: `1` at a quarter of a second, cheaper with more
     * time (down to [URGENCY_MIN]) and dearer with less (up to [URGENCY_MAX]).
     */
    fun urgency(available: Double): Double =
        if (available <= 0.0) URGENCY_MAX else (URGENCY_REFERENCE / available).coerceIn(URGENCY_MIN, URGENCY_MAX)

    private fun interval(ctx: DecodeContext, candidate: Candidate): Int =
        kotlin.math.abs(ctx.notes[candidate.notes.last()].pitch - ctx.notes[candidate.notes.first()].pitch)

    /**
     * The cost of [candidate] on its own, under weights [w]. How high the hand sits is charged by the decoder
     * instead, on the hand's position rather than the notes', so that an open string doesn't look like a way to
     * bring the hand down the neck.
     */
    fun staticCost(ctx: DecodeContext, candidate: Candidate, w: FrettingWeights, breakdown: CostBreakdown?): Double {
        val profile = ctx.profile
        var total = 0.0

        fun add(term: CostTerm, value: Double) {
            if (value == 0.0) return
            total += value
            breakdown?.add(term, value)
        }

        if (candidate.minFret >= 0) {
            if (candidate.spanMm > profile.comfortableSpanMm) {
                val over = (candidate.spanMm - profile.comfortableSpanMm) / 10.0
                add(CostTerm.STRETCH, w.stretch * over * over)
            }
            add(CostTerm.ACCESS, w.pastAccess * max(0, candidate.maxFret - profile.accessFret))
        }
        add(CostTerm.FINGERS, w.finger * candidate.fingerCount)
        if (candidate.barreFret >= 0) {
            val widest = (profile.stringCount - 2).coerceAtLeast(1)
            add(CostTerm.BARRE, w.barre * (0.25 + 0.75 * (candidate.barreWidth - 2).coerceAtLeast(0) / widest))
        }

        var open = 0
        var harmonics = 0
        var bend = 0.0
        for (k in candidate.notes.indices) {
            val note = ctx.notes[candidate.notes[k]]
            val isOpen = candidate.frets[k] == ctx.capo && !candidate.harmonic[k]
            if (isOpen) open++
            if (candidate.harmonic[k]) harmonics++
            if (note.bend >= REAL_BEND) {
                val impossible = isOpen || candidate.harmonic[k] ||
                    candidate.frets[k] + note.bendUp > profile.fretCount + 1e-9 ||
                    candidate.frets[k] - note.bendDown < -1e-9
                bend += when {
                    impossible -> w.bendImpossible
                    // The further below the usual bending strings, the less likely.
                    profile.bendStrings != null && candidate.strings[k] < profile.bendStrings.first ->
                        w.bendLowString * (profile.bendStrings.first - candidate.strings[k])
                    else -> 0.0
                }
            } else if (note.bend > VIBRATO && isOpen) {
                bend += w.vibratoOpen
            }
        }
        add(CostTerm.OPEN, w.openString * open)
        add(CostTerm.HARMONIC, w.harmonic * harmonics)
        add(CostTerm.BEND, bend)

        if (candidate.notes.size >= 2) {
            val low = candidate.strings.min()
            val high = candidate.strings.max()
            val muted = (high - low + 1) - candidate.notes.size
            if (candidate.notes.size >= 3) {
                add(CostTerm.INNER_MUTE, w.innerMute * muted)
            } else if (interval(ctx, candidate) < OCTAVE) {
                // Two-note shapes skip a string only for octaves and wider; a fifth or a third sits on adjacent strings.
                add(CostTerm.INNER_MUTE, w.innerMute * DYAD_SKIP_FACTOR * muted)
            }
        }
        add(CostTerm.DROP, w.drop * candidate.dropPenalty)
        return total
    }
}
