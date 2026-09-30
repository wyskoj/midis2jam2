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

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * Where one note is played.
 *
 * @property string The string, lowest (thickest) first.
 * @property fret The fret, counted from the nut. A capo'd open string is at the capo's fret.
 * @property finger The finger pressing it: `0` for an open string, `1` (index) to `4` (little).
 * @property isHarmonic Whether the note is a natural harmonic touched over [fret].
 */
data class Fingering(val string: Int, val fret: Int, val finger: Int, val isHarmonic: Boolean = false)

/**
 * Where the fretting hand is from [time] on: the index finger over [indexFret], barring [barreFret] (or `-1`).
 *
 * The keyframes are for animating a hand; nothing draws one yet.
 */
data class HandKeyframe(val time: Double, val indexFret: Int, val barreFret: Int)

/**
 * What the engine inferred about the whole part, for the live readout.
 *
 * @property ranking Every tuning and capo considered, best first.
 * @property noteCount How many notes the part has.
 * @property sliceCount How many slices (chords, strums and single notes) the part was split into.
 * @property droppedCount How many notes couldn't be fingered and aren't shown.
 * @property stealCount How often a note took a string that was still noticeably ringing.
 * @property riffFamilies How many repeated passages were fingered consistently by the riff memory.
 * @property decodeMillis How long the whole solve took.
 */
class GlobalDiagnostics(
    val ranking: List<TuningScore>,
    val noteCount: Int,
    val sliceCount: Int,
    val droppedCount: Int,
    val stealCount: Int,
    val riffFamilies: Int,
    val decodeMillis: Long,
)

/**
 * What the engine decided for one slice, for the live readout.
 *
 * @property start When the slice starts, in seconds.
 * @property end When the next slice starts (or the last note ends).
 * @property leadness From `0` (rhythm) to `1` (lead).
 * @property phraseStart Whether the slice begins a phrase.
 * @property hand The index finger's fret, or `-1` if the hand hasn't been needed yet.
 * @property barreFret The barred fret, or `-1`.
 * @property shift How far the hand moved to get here, in frets (negative is toward the nut).
 * @property urgency How rushed that move was (see [CostModel.urgency]); `0` if the hand didn't move.
 * @property frets Per string, the fret played in this slice, or `-1`.
 * @property fingers Per string, the finger used in this slice, or `-1`.
 * @property breakdown The slice's cost, term by term.
 */
class SliceDiagnostics(
    val start: Double,
    val end: Double,
    val leadness: Double,
    val phraseStart: Boolean,
    val hand: Int,
    val barreFret: Int,
    val shift: Int,
    val urgency: Double,
    val frets: IntArray,
    val fingers: IntArray,
    val breakdown: CostBreakdown,
)

/**
 * How to play a whole part.
 *
 * @property profile The instrument it was solved for.
 * @property tuning The tuning chosen (or given).
 * @property capo The capo's fret, or `0` for none.
 * @property fingerings Per input note, where it is played, or `null` if it couldn't be fingered.
 * @property hand Where the fretting hand is over time.
 * @property cost The total cost of the chosen fingerings.
 * @property global What was inferred about the whole part.
 * @property slices What was decided for each slice, in time order.
 */
class FrettingSolution(
    val profile: FrettingProfile,
    val tuning: Tuning,
    val capo: Int,
    val fingerings: List<Fingering?>,
    val hand: List<HandKeyframe>,
    val cost: Double,
    val global: GlobalDiagnostics,
    val slices: List<SliceDiagnostics>,
)

/**
 * Works out how a string player would finger a part.
 *
 * The whole part is known in advance, so rather than deciding note by note, [solve] finds the cheapest path through
 * the whole song:
 *  1. [OnsetSlicer] groups notes that start together;
 *  2. [TextureAnalyzer] labels each slice as rhythm or lead, and finds phrase boundaries;
 *  3. [TuningSelector] picks the tuning and capo (unless one is given);
 *  4. [BeamDecoder] finds the cheapest sequence of [CandidateGenerator]'s fingerings;
 *  5. repeated passages that came out fingered differently are decoded again, rewarding one consistent fingering.
 */
object Fretter {
    private const val RIFF_CONTEXT_BEFORE = 3
    private const val RIFF_CONTEXT_AFTER = 4

    /**
     * Solves [notes] for [profile]. With [tuning] given, no tuning or capo is inferred and [capo] is used as is.
     */
    fun solve(
        notes: List<FrettingNote>,
        profile: FrettingProfile,
        tuning: Tuning? = null,
        capo: Int = 0,
        beamWidth: Int = BeamDecoder.DEFAULT_BEAM,
    ): FrettingSolution {
        val started = TimeSource.Monotonic.markNow()
        val slices = OnsetSlicer.slice(notes, profile.stringCount)
        val texture = TextureAnalyzer.analyze(notes, slices)
        val weights = slices.indices.map { FrettingWeights.blend(profile.rhythm, profile.lead, texture.leadness[it]) }

        val ranking = if (tuning != null) {
            listOf(TuningScore(tuning, capo, 0.0, 0.0, TuningSelector.unplayable(notes, profile, tuning, capo)))
        } else {
            TuningSelector.rank(notes, slices, texture, weights, profile)
        }
        val chosen = ranking.first()
        val ctx = DecodeContext(notes, slices, profile, texture, weights, chosen.tuning, chosen.capo)

        var preferred = emptyMap<Int, List<Int>>()
        var result = BeamDecoder.decode(ctx, beamWidth = beamWidth)
        var families = 0
        if (slices.isNotEmpty()) {
            val riffs = riffPreferences(ctx, result)
            if (riffs.preferred.isNotEmpty()) {
                preferred = riffs.preferred
                families = riffs.families
                result = BeamDecoder.decode(ctx, beamWidth = beamWidth, preferred = preferred)
            }
        }

        val path = result.path
        val fingerings = arrayOfNulls<Fingering>(notes.size)
        val sliceDiagnostics = ArrayList<SliceDiagnostics>(path.size)
        val hand = mutableListOf<HandKeyframe>()
        var steals = 0
        path.forEachIndexed { index, hypothesis ->
            val candidate = hypothesis.candidate!!
            val slice = slices[hypothesis.slice]
            val fingers = CandidateGenerator.assignFingers(candidate, hypothesis.hand, ctx.capo)
            val fretsByString = IntArray(profile.stringCount) { -1 }
            val fingersByString = IntArray(profile.stringCount) { -1 }
            for (k in candidate.notes.indices) {
                fingerings[candidate.notes[k]] =
                    Fingering(candidate.strings[k], candidate.frets[k], fingers[k], candidate.harmonic[k])
                fretsByString[candidate.strings[k]] = candidate.frets[k]
                fingersByString[candidate.strings[k]] = fingers[k]
            }
            steals += hypothesis.steals
            if (candidate.usesHand && hand.lastOrNull().let { it == null || it.indexFret != hypothesis.hand || it.barreFret != candidate.barreFret }) {
                hand += HandKeyframe(slice.time, hypothesis.hand, candidate.barreFret)
            }
            val end = slices.getOrNull(hypothesis.slice + 1)?.time ?: slice.notes.maxOf { notes[it].end }
            sliceDiagnostics += SliceDiagnostics(
                start = slice.time,
                end = end,
                leadness = texture.leadness[hypothesis.slice],
                phraseStart = texture.phraseStart[hypothesis.slice],
                hand = hypothesis.hand,
                barreFret = candidate.barreFret,
                shift = if (index == 0) 0 else hypothesis.shift,
                urgency = hypothesis.urgency,
                frets = fretsByString,
                fingers = fingersByString,
                breakdown = BeamDecoder.explain(ctx, hypothesis, preferred),
            )
        }

        val global = GlobalDiagnostics(
            ranking = ranking,
            noteCount = notes.size,
            sliceCount = slices.size,
            droppedCount = fingerings.count { it == null },
            stealCount = steals,
            riffFamilies = families,
            decodeMillis = started.elapsedNow().inWholeMilliseconds,
        )
        return FrettingSolution(
            profile, chosen.tuning, chosen.capo, fingerings.toList(), hand, result.cost, global, sliceDiagnostics,
        )
    }

    private class RiffPreferences(val preferred: Map<Int, List<Int>>, val families: Int)

    /**
     * Finds passages that repeat — the same notes in the same rhythm, judged over a few slices either side — and,
     * where the first decode fingered their occurrences differently, prefers the fingering most occurrences used.
     */
    private fun riffPreferences(ctx: DecodeContext, first: DecodeResult): RiffPreferences {
        val slices = ctx.slices
        val tokens = IntArray(slices.size) { i ->
            val pitches = slices[i].notes.map { ctx.notes[it].pitch }
            val gap = (slices.getOrNull(i + 1)?.time ?: (slices[i].time + 1.0)) - slices[i].time
            val rhythm = if (gap <= 0.0) 0 else (ln(gap) / ln(2.0) * 2).roundToInt()
            pitches.hashCode() * 31 + rhythm
        }
        val families = mutableMapOf<List<Int>, MutableList<Int>>()
        for (i in slices.indices) {
            if (i - RIFF_CONTEXT_BEFORE < 0 || i + RIFF_CONTEXT_AFTER >= slices.size) continue
            val context = (i - RIFF_CONTEXT_BEFORE..i + RIFF_CONTEXT_AFTER).map { tokens[it] }
            families.getOrPut(context) { mutableListOf() } += i
        }
        val chosen = first.path.associate { it.slice to it.candidate!!.key }
        val preferred = mutableMapOf<Int, List<Int>>()
        var inconsistent = 0
        for (occurrences in families.values) {
            if (occurrences.size < 2) continue
            val keys = occurrences.mapNotNull { chosen[it] }
            if (keys.distinct().size <= 1) continue
            inconsistent++
            val counts = keys.groupingBy { it }.eachCount()
            // The most common fingering, and among equally common ones the earliest.
            val best = keys.maxWith(compareBy<List<Int>> { counts.getValue(it) }.thenBy { -keys.indexOf(it) })
            occurrences.forEach { preferred[it] = best }
        }
        return RiffPreferences(preferred, inconsistent)
    }
}
