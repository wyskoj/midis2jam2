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

import kotlin.math.abs
import kotlin.math.min

/**
 * Everything one decode needs: the notes and their slices, the instrument, the tuning and capo under test, and the
 * weights for each slice.
 */
class DecodeContext(
    val notes: List<FrettingNote>,
    val slices: List<Slice>,
    val profile: FrettingProfile,
    val texture: Texture,
    val weights: List<FrettingWeights>,
    val tuning: Tuning,
    val capo: Int,
    val maxCandidates: Int = CandidateGenerator.DEFAULT_MAX_CANDIDATES,
) {
    private val cache = arrayOfNulls<List<Candidate>>(slices.size)

    /** For each index-finger fret, the highest fret the hand reaches comfortably. */
    val reach: IntArray = IntArray(profile.fretCount + 1) {
        profile.geometry.reach(it, profile.comfortableSpanMm, profile.fretCount)
    }

    /** The candidate fingerings of slice [index], generated once. */
    fun candidates(index: Int): List<Candidate> =
        cache[index] ?: CandidateGenerator.generate(this, index, maxCandidates).also { cache[index] = it }
}

/**
 * A partial path through the song: the fingerings chosen so far, and where that leaves the hand and the strings.
 *
 * @property hand The fret of the index finger, or `-1` before the hand has been needed.
 * @property handTime When the hand was last needed.
 * @property occupantNote Per string, the note ringing on it, or `-1`.
 * @property occupantEnd Per string, when that note stops ringing.
 * @property occupantFret Per string, the fret that note is held at.
 */
internal class Hypothesis(
    val parent: Hypothesis?,
    val slice: Int,
    val candidate: Candidate?,
    val cost: Double,
    val hand: Int,
    val handTime: Double,
    val occupantNote: IntArray,
    val occupantEnd: DoubleArray,
    val occupantFret: IntArray,
    val shift: Int,
    val urgency: Double,
    val steals: Int,
)

/**
 * The result of decoding a run of slices.
 *
 * @property cost The total cost of the best path.
 */
internal class DecodeResult(private val last: Hypothesis, val cost: Double) {
    /** The hypotheses of the best path, one per decoded slice, in order. */
    val path: List<Hypothesis> by lazy {
        generateSequence(last) { it.parent }.filter { it.candidate != null }.toList().reversed()
    }
}

/**
 * Finds the cheapest sequence of fingerings for a run of slices, by beam search.
 *
 * Every hypothesis carries the state that later costs depend on: where the hand is, when it was last needed, and
 * which note rings on each string until when. Two hypotheses that reach the same fingering in the same state are
 * merged, keeping the cheaper, so where the state repeats the search is exact dynamic programming.
 */
internal object BeamDecoder {
    /** How many hypotheses survive each slice. */
    const val DEFAULT_BEAM: Int = 48

    private const val PHRASE_SHIFT_DISCOUNT = 0.3
    private const val LEGATO_GAP = 0.02
    private const val LEGATO_WINDOW = 0.4
    private const val LEGATO_MAX_INTERVAL = 4
    private const val NOTICEABLE_STEAL = 0.05
    private const val NEVER = -1e9

    private class Key(val candidate: Int, val hand: Int, val occupants: IntArray) {
        override fun equals(other: Any?): Boolean =
            other is Key && other.candidate == candidate && other.hand == hand && other.occupants.contentEquals(occupants)

        override fun hashCode(): Int = (candidate * 31 + hand) * 31 + occupants.contentHashCode()
    }

    /** The state and cost a transition leads to, before it is stored. */
    private class Step {
        var cost = 0.0
        var hand = -1
        var handTime = NEVER
        var shift = 0
        var urgency = 0.0
        var steals = 0
        var released = 0
    }

    /**
     * Decodes slices [range] of [ctx] with [beamWidth] hypotheses, rewarding [preferred] fingerings (by slice) with the
     * riff weight.
     */
    fun decode(
        ctx: DecodeContext,
        range: IntRange = ctx.slices.indices,
        beamWidth: Int = DEFAULT_BEAM,
        preferred: Map<Int, List<Int>> = emptyMap(),
    ): DecodeResult {
        val strings = ctx.profile.stringCount
        var beam = listOf(
            Hypothesis(
                parent = null, slice = -1, candidate = null, cost = 0.0, hand = -1, handTime = NEVER,
                occupantNote = IntArray(strings) { -1 }, occupantEnd = DoubleArray(strings), occupantFret = IntArray(strings) { -1 },
                shift = 0, urgency = 0.0, steals = 0,
            ),
        )
        val step = Step()
        for (i in range) {
            val candidates = ctx.candidates(i)
            val w = ctx.weights[i]
            val nextTime = ctx.slices.getOrNull(i + 1)?.time ?: Double.MAX_VALUE
            val preferredKey = preferred[i]
            val next = LinkedHashMap<Key, Hypothesis>(beam.size * candidates.size * 2)
            for (previous in beam) {
                for (candidate in candidates) {
                    transition(ctx, i, previous, candidate, w, step, null)
                    val riff = if (preferredKey != null && candidate.key == preferredKey) w.riff else 0.0
                    val total = previous.cost + candidate.staticCost + riff + step.cost
                    val occupants = occupantsAfter(ctx, previous, candidate, step.released, nextTime)
                    val key = Key(candidate.id, step.hand, occupants)
                    val existing = next[key]
                    if (existing == null || total < existing.cost) {
                        next[key] = materialize(ctx, previous, i, candidate, total, step, occupants)
                    }
                }
            }
            beam = next.values.sortedWith(compareBy { it.cost }).take(beamWidth)
        }
        val best = beam.first()
        return DecodeResult(best, best.cost)
    }

    private fun occupantsAfter(
        ctx: DecodeContext,
        previous: Hypothesis,
        candidate: Candidate,
        released: Int,
        nextTime: Double,
    ): IntArray {
        val occupants = previous.occupantNote.copyOf()
        for (s in occupants.indices) {
            if (occupants[s] >= 0 && (released and (1 shl s) != 0 || previous.occupantEnd[s] <= nextTime)) occupants[s] = -1
        }
        for (k in candidate.notes.indices) {
            val note = candidate.notes[k]
            occupants[candidate.strings[k]] = if (ctx.notes[note].end > nextTime) note else -1
        }
        return occupants
    }

    private fun materialize(
        ctx: DecodeContext,
        previous: Hypothesis,
        slice: Int,
        candidate: Candidate,
        cost: Double,
        step: Step,
        occupants: IntArray,
    ): Hypothesis {
        val ends = DoubleArray(occupants.size)
        val frets = IntArray(occupants.size) { -1 }
        for (s in occupants.indices) {
            if (occupants[s] < 0) continue
            ends[s] = ctx.notes[occupants[s]].end
            frets[s] = previous.occupantFret[s]
        }
        for (k in candidate.notes.indices) {
            val s = candidate.strings[k]
            if (occupants[s] == candidate.notes[k]) frets[s] = candidate.frets[k]
        }
        return Hypothesis(
            parent = previous, slice = slice, candidate = candidate, cost = cost, hand = step.hand,
            handTime = step.handTime, occupantNote = occupants, occupantEnd = ends, occupantFret = frets,
            shift = step.shift, urgency = step.urgency, steals = step.steals,
        )
    }

    /** Works out the cost of moving from [previous] to [candidate] at slice [i], and the state it leads to. */
    private fun transition(
        ctx: DecodeContext,
        i: Int,
        previous: Hypothesis,
        candidate: Candidate,
        w: FrettingWeights,
        out: Step,
        breakdown: CostBreakdown?,
    ) {
        val slice = ctx.slices[i]
        val t = slice.time
        var cost = 0.0

        fun add(term: CostTerm, value: Double) {
            if (value == 0.0) return
            cost += value
            breakdown?.add(term, value)
        }

        // Where the hand goes, and what moving it costs.
        var hand = previous.hand
        var handTime = previous.handTime
        var shift = 0
        var urgency = 0.0
        if (candidate.usesHand) {
            hand = when {
                previous.hand < 0 -> candidate.minFret
                // Chord shapes are fingered from the index finger; only single notes (and one-finger double stops)
                // are reached with the other fingers from wherever the hand already is.
                candidate.fingerCount > 1 -> candidate.minFret
                candidate.minFret >= previous.hand && candidate.maxFret <= ctx.reach[previous.hand] -> previous.hand
                candidate.minFret < previous.hand -> candidate.minFret
                else -> {
                    // Move up only as far as needed to reach the highest note, never past the lowest.
                    var p = previous.hand
                    while (p < candidate.minFret && ctx.reach[p] < candidate.maxFret) p++
                    p
                }
            }
            if (previous.hand >= 0) {
                shift = hand - previous.hand
                if (shift != 0) {
                    urgency = CostModel.urgency(t - previous.handTime)
                    val phrase = ctx.texture.phraseStart[i]
                    add(
                        CostTerm.SHIFT,
                        w.shift * abs(shift) * urgency * (if (phrase) PHRASE_SHIFT_DISCOUNT else 1.0) + w.shiftOnset,
                    )
                    if (!phrase) add(CostTerm.STICKINESS, w.stickiness)
                }
            }
            handTime = t
        }
        if (hand >= 0) add(CostTerm.POSITION, w.position * abs(hand - ctx.capo - w.positionTarget))

        val before = previous.candidate
        if (before != null && before.notes.isNotEmpty() && candidate.notes.isNotEmpty()) {
            // Picking-hand string crossings between single notes (and double stops).
            if (candidate.notes.size <= 2 && before.notes.size <= 2) {
                val crossed = abs(candidate.strings.min() - before.strings.min())
                if (crossed > 0) add(CostTerm.CROSS, w.cross * crossed * CostModel.urgency(t - ctx.slices[previous.slice].time))
            }

            // A repeated pitch played in the same place.
            for (k in candidate.notes.indices) {
                val pitch = ctx.notes[candidate.notes[k]].pitch
                for (j in before.notes.indices) {
                    if (ctx.notes[before.notes[j]].pitch == pitch &&
                        before.strings[j] == candidate.strings[k] && before.frets[j] == candidate.frets[k]
                    ) {
                        add(CostTerm.REPEAT, w.repeat)
                    }
                }
            }

            // A chord shape moved along the same strings.
            if (before.notes.size >= 2 && before.notes.size == candidate.notes.size &&
                before.strings.contentEquals(candidate.strings)
            ) {
                // Every note moves by the same amount, so the hand keeps its shape. Only the root may be an open string
                // (E5 at the nut slid up to G5 is the same shape); other open strings aren't part of a movable shape.
                val delta = candidate.frets[0] - before.frets[0]
                val moved = delta != 0 && candidate.notes.indices.all {
                    candidate.frets[it] - before.frets[it] == delta && !candidate.harmonic[it] && !before.harmonic[it] &&
                        (it == 0 || (candidate.frets[it] > ctx.capo && before.frets[it] > ctx.capo))
                }
                if (moved) add(CostTerm.SHAPE, w.shape)
            }

            // Hammer-ons, pull-offs, slides and trills stay on one string.
            if (candidate.notes.size == 1 && before.notes.size == 1) {
                val now = ctx.notes[candidate.notes[0]]
                val then = ctx.notes[before.notes[0]]
                val interval = abs(now.pitch - then.pitch)
                if (interval in 1..LEGATO_MAX_INTERVAL && now.start - then.end < LEGATO_GAP &&
                    now.start - then.start < LEGATO_WINDOW && candidate.strings[0] == before.strings[0]
                ) {
                    add(CostTerm.LEGATO, w.legato)
                }
            }
        }

        // Taking a string that is still ringing cuts that note short.
        var steals = 0
        for (k in candidate.notes.indices) {
            val s = candidate.strings[k]
            if (previous.occupantNote[s] >= 0 && previous.occupantEnd[s] > t) {
                val remaining = min(previous.occupantEnd[s] - t, 1.0)
                add(CostTerm.STEAL, w.steal * remaining)
                if (remaining > NOTICEABLE_STEAL) steals++
            }
        }

        // Moving the hand away from a held fretted note cuts that note short, too.
        var released = 0
        if (shift != 0) {
            var used = 0
            candidate.strings.forEach { used = used or (1 shl it) }
            for (s in 0 until ctx.profile.stringCount) {
                if (used and (1 shl s) != 0) continue
                if (previous.occupantNote[s] < 0 || previous.occupantEnd[s] <= t) continue
                val fret = previous.occupantFret[s]
                if (fret <= ctx.capo) continue
                if (fret < hand || fret > ctx.reach[hand]) {
                    add(CostTerm.RELEASE, w.release * min(previous.occupantEnd[s] - t, 1.0))
                    released = released or (1 shl s)
                }
            }
        }

        out.cost = cost
        out.hand = hand
        out.handTime = handTime
        out.shift = shift
        out.urgency = urgency
        out.steals = steals
        out.released = released
    }

    /** Re-scores [hypothesis] term by term, to explain why it was chosen. */
    fun explain(ctx: DecodeContext, hypothesis: Hypothesis, preferred: Map<Int, List<Int>>): CostBreakdown {
        val breakdown = CostBreakdown()
        val candidate = hypothesis.candidate ?: return breakdown
        val w = ctx.weights[hypothesis.slice]
        CostModel.staticCost(ctx, candidate, w, breakdown)
        if (preferred[hypothesis.slice] == candidate.key) breakdown.add(CostTerm.RIFF, w.riff)
        hypothesis.parent?.let { transition(ctx, hypothesis.slice, it, candidate, w, Step(), breakdown) }
        return breakdown
    }
}
