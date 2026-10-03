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
 * How well one tuning and capo fit the music.
 *
 * @property score Lower is better: the cost of playing a sample of the music this way, plus [prior].
 * @property prior The part of [score] that is the tuning's and capo's own unlikelihood.
 * @property unplayable How many notes of the whole part have nowhere to be played this way.
 */
data class TuningScore(val tuning: Tuning, val capo: Int, val score: Double, val prior: Double, val unplayable: Int) {
    /** The tuning and capo, for display, e.g. `Drop D` or `E std capo 2`. */
    val label: String get() = if (capo == 0) tuning.displayName else "${tuning.displayName} capo $capo"
}

/**
 * How a tuning and capo are chosen.
 *
 * @property openString The least an open string is rewarded while choosing. Alternate tunings and capos exist to make
 * open strings available and barres unnecessary, so while choosing one they count for more than players favour them
 * when simply fingering a part (which is what a profile's own weights describe).
 * @property barre The least a full barre costs while choosing.
 * @property priorPerSlice How a tuning's or capo's prior scales with the length of the music sampled, so that a small
 * advantage per chord, repeated through a long song, is needed to overcome it.
 * @property minPriorScale The least the prior is scaled by, for short parts.
 * @property unplayableNote The cost of each note, anywhere in the part, that has nowhere to be played. Unlike the
 * decode costs, this counts the whole part rather than a sample: a recurring low D is decisive evidence of drop D
 * however rarely it falls in the sampled windows.
 * @property capoPrior How unlikely a capo is before looking at the music, plus [capoPriorPerFret] for each fret.
 * @property capoPriorPerFret See [capoPrior].
 *
 * The defaults were chosen by sweeping against GuitarSet (every part in standard tuning without a capo, so anything
 * else is a false alarm) and AnimeTAB (arrangements in a variety of tunings and capos); see `FrettingCorpusBenchmark`.
 * A capo is only inferred on strong evidence: a missed capo still gives a realistic fingering, an invented one
 * doesn't.
 */
data class TuningSelection(
    val openString: Double = -0.2,
    val barre: Double = 0.6,
    val priorPerSlice: Double = 0.08,
    val minPriorScale: Double = 5.0,
    val unplayableNote: Double = 8.0,
    val capoPrior: Double = 2.5,
    val capoPriorPerFret: Double = 0.25,
)

/**
 * Picks the tuning and capo that make a part easiest to play, weighted toward the common ones.
 *
 * Each tuning in the profile's catalogue is scored by decoding a sample of the music in it, plus its prior, plus a
 * cost for every note of the part it can't play at all (see [TuningSelection]). The best tunings are then tried with
 * a capo, if the instrument uses one.
 */
object TuningSelector {
    private const val SAMPLE_WINDOWS = 6
    private const val SAMPLE_WINDOW_SLICES = 40
    private const val QUICK_BEAM = 12
    private const val QUICK_CANDIDATES = 12
    private const val CAPO_TUNINGS = 2
    private const val HOPELESS_MARGIN = 0.02

    /** Scores every tuning and capo worth considering for [notes], best first. */
    fun rank(
        notes: List<FrettingNote>,
        slices: List<Slice>,
        texture: Texture,
        profileWeights: List<FrettingWeights>,
        profile: FrettingProfile,
    ): List<TuningScore> {
        if (slices.isEmpty() || profile.tunings.size == 1 && profile.capoRange.last == 0) {
            return listOf(TuningScore(profile.defaultTuning, 0, 0.0, 0.0, 0))
        }
        val selection = profile.selection
        val weights = profileWeights.map {
            it.copy(openString = minOf(it.openString, selection.openString), barre = maxOf(it.barre, selection.barre))
        }
        val windows = sampleWindows(slices.size)
        val sampled = windows.sumOf { it.count() }
        val priorScale = max(selection.minPriorScale, selection.priorPerSlice * sampled)
        val sampledNotes = windows.flatMap { range -> range.flatMap { slices[it].notes.asIterable() } }

        fun score(tuning: Tuning, capo: Int): TuningScore {
            val ctx = DecodeContext(notes, slices, profile, texture, weights, tuning, capo, QUICK_CANDIDATES)
            val decoded = windows.sumOf { BeamDecoder.decode(ctx, it, QUICK_BEAM).cost }
            // Unplayable notes in the sample were charged the drop weight by every candidate; charge them once, below,
            // over the whole part instead.
            val sampledUnplayable = sampledNotes.count { !isPlayable(notes[it], profile, tuning, capo) }
            val cost = decoded - sampledUnplayable * weights.first().drop
            val unplayable = unplayable(notes, profile, tuning, capo)
            val capoPrior = if (capo > 0) selection.capoPrior + selection.capoPriorPerFret * capo else 0.0
            val prior = (tuning.prior + capoPrior) * priorScale
            return TuningScore(tuning, capo, cost + prior + unplayable * selection.unplayableNote, prior, unplayable)
        }

        val tolerance = max(3, (notes.size * HOPELESS_MARGIN).toInt())
        val fewestUnplayable = profile.tunings.minOf { unplayable(notes, profile, it, 0) }
        val scores = profile.tunings
            .filter { unplayable(notes, profile, it, 0) <= fewestUnplayable + tolerance }
            .map { score(it, 0) }
            .toMutableList()

        if (profile.capoRange.last > 0) {
            scores.sortedBy { it.score }.take(CAPO_TUNINGS).forEach { base ->
                for (capo in profile.capoRange) {
                    if (capo == 0) continue
                    if (unplayable(notes, profile, base.tuning, capo) > fewestUnplayable + tolerance) break
                    scores += score(base.tuning, capo)
                }
            }
        }
        return scores.sortedWith(compareBy({ it.score }, { it.prior }))
    }

    /** How many of [notes] have no position at all in [tuning] with [capo]. */
    fun unplayable(notes: List<FrettingNote>, profile: FrettingProfile, tuning: Tuning, capo: Int): Int =
        notes.count { !isPlayable(it, profile, tuning, capo) }

    private fun isPlayable(note: FrettingNote, profile: FrettingProfile, tuning: Tuning, capo: Int): Boolean {
        val highestOpen = (0 until profile.stringCount).maxOf { tuning[it] }
        val highest = maxOf(highestOpen + profile.fretCount, if (profile.harmonics) highestOpen + capo + 24 else 0)
        return note.pitch >= tuning.lowest + capo && note.pitch <= highest
    }

    private fun sampleWindows(sliceCount: Int): List<IntRange> {
        val total = SAMPLE_WINDOWS * SAMPLE_WINDOW_SLICES
        if (sliceCount <= total) return listOf(0 until sliceCount)
        val last = sliceCount - SAMPLE_WINDOW_SLICES
        return (0 until SAMPLE_WINDOWS).map { k ->
            val start = (last.toLong() * k / (SAMPLE_WINDOWS - 1)).toInt()
            start until start + SAMPLE_WINDOW_SLICES
        }
    }
}
