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
 * One way to finger a [Slice]. The arrays run in parallel, one entry per played note, lowest pitch first.
 *
 * @property id The candidate's rank within its slice, cheapest first.
 * @property notes Indices of the notes played.
 * @property strings The string each note is played on.
 * @property frets The fret each note is played at, counted from the nut (so a capo'd open string is at the capo).
 * For a natural harmonic, the fret the harmonic is touched over.
 * @property harmonic Whether each note is played as a natural harmonic.
 * @property dropped Indices of notes in the slice that couldn't be fingered and are left out.
 * @property dropPenalty How much leaving out [dropped] costs, in multiples of the drop weight.
 * @property minFret The lowest fretted fret, or `-1` when every note is open.
 * @property maxFret The highest fretted fret, or `-1` when every note is open.
 * @property barreFret The fret barred by the index finger, or `-1` for no barre.
 * @property barreWidth How many strings the barre covers.
 * @property fingerCount How many fingers the fretting hand uses.
 * @property spanMm How far the fretting hand spans.
 */
class Candidate(
    val id: Int,
    val notes: IntArray,
    val strings: IntArray,
    val frets: IntArray,
    val harmonic: BooleanArray,
    val dropped: IntArray,
    val dropPenalty: Double,
    val minFret: Int,
    val maxFret: Int,
    val barreFret: Int,
    val barreWidth: Int,
    val fingerCount: Int,
    val spanMm: Double,
) {
    /** The cost of this fingering on its own; see [CostModel.staticCost]. */
    var staticCost: Double = 0.0
        internal set

    /** Identifies the fingering (which strings at which frets), to compare repeated passages. */
    val key: List<Int> by lazy { strings.indices.map { strings[it] * 1024 + frets[it] * 2 + if (harmonic[it]) 1 else 0 }.sorted() }

    /** Whether any note needs the fretting hand. */
    val usesHand: Boolean get() = minFret >= 0

    /** Returns a copy with a different [id]. */
    internal fun withId(id: Int): Candidate = Candidate(
        id, notes, strings, frets, harmonic, dropped, dropPenalty, minFret, maxFret, barreFret, barreWidth,
        fingerCount, spanMm,
    ).also { it.staticCost = staticCost }

    /** Returns a copy whose [notes] and [dropped] are mapped through [map]. */
    internal fun withNotes(map: (Int) -> Int): Candidate = Candidate(
        id, IntArray(notes.size) { map(notes[it]) }, strings, frets, harmonic, IntArray(dropped.size) { map(dropped[it]) },
        dropPenalty, minFret, maxFret, barreFret, barreWidth, fingerCount, spanMm,
    )
}

/**
 * Lists the playable fingerings of a slice.
 *
 * Notes are assigned to distinct strings by backtracking, abandoning any partial assignment the hand can't span.
 * A fingering needing more than four fingers is only allowed with a barre, and on a bowed instrument the strings
 * must be adjacent. If no fingering plays every note — more notes than strings, or a stretch no hand makes — notes
 * are left out, as few as possible: octave doublings are the cheapest to lose, then inner voices, and the bass and
 * the top note the dearest.
 */
object CandidateGenerator {
    /** How many candidates to keep per slice. */
    const val DEFAULT_MAX_CANDIDATES: Int = 24

    private const val MAX_LEAVES = 20_000
    private const val MAX_FINGERS = 4
    private const val MAX_DROP_SETS = 64
    private const val INNER_DROP = 1.3
    private const val OUTER_DROP = 2.0

    private class Position(val string: Int, val fret: Int, val harmonic: Boolean)

    /**
     * The fingerings of slice [sliceIndex], cheapest first, at most [maxCandidates] of them.
     *
     * Where they can go depends only on the slice's pitches, so slices with the same pitches (the same chord played
     * again) share the search through [cache], which is keyed by pitches and holds fingerings by each note's place in
     * its slice. Only the costs, which also depend on bends and on the slice's weights, are worked out each time.
     */
    fun generate(
        ctx: DecodeContext,
        sliceIndex: Int,
        maxCandidates: Int = DEFAULT_MAX_CANDIDATES,
        cache: MutableMap<List<Int>, List<Candidate>>? = null,
    ): List<Candidate> {
        val slice = ctx.slices[sliceIndex]
        val w = ctx.weights[sliceIndex]
        val pitches = slice.notes.map { ctx.notes[it].pitch }
        val result = cache?.get(pitches)?.map { candidate -> candidate.withNotes { slice.notes[it] } }
            ?: search(ctx, slice).also { found ->
                cache?.put(pitches, found.map { candidate -> candidate.withNotes { slice.notes.indexOf(it) } })
            }
        result.forEach { it.staticCost = CostModel.staticCost(ctx, it, w, null) }
        return result
            .withIndex()
            .sortedWith(compareBy({ it.value.staticCost }, { it.index }))
            .take(maxCandidates)
            .mapIndexed { rank, it -> it.value.withId(rank) }
    }

    /** Every fingering of [slice], in the order found, without costs. */
    private fun search(ctx: DecodeContext, slice: Slice): List<Candidate> {
        val positions = slice.notes.associateWith { positionsOf(ctx, ctx.notes[it].pitch) }
        val unplayable = slice.notes.filter { positions.getValue(it).isEmpty() }
        val playable = slice.notes.filter { positions.getValue(it).isNotEmpty() }
        val penalties = dropPenalties(ctx, playable)
        val dropOrder = playable.sortedWith(compareBy({ penalties.getValue(it) }, { dropPriority(ctx, playable, it) }))

        var result: List<Candidate> = emptyList()
        var dropCount = (playable.size - ctx.profile.stringCount).coerceAtLeast(0)
        while (result.isEmpty() && dropCount < playable.size) {
            // Try leaving out each set of dropCount notes, most expendable sets first.
            val found = mutableListOf<Candidate>()
            combinations(dropOrder, dropCount, MAX_DROP_SETS) { dropped ->
                val kept = playable.filter { it !in dropped }
                val penalty = unplayable.size + dropped.sumOf { penalties.getValue(it) }
                found += enumerate(ctx, kept, positions, (unplayable + dropped).toIntArray(), penalty)
            }
            result = found
            dropCount++
        }
        if (result.isEmpty()) {
            result = listOf(empty(slice.notes.toList(), unplayable.size + playable.sumOf { penalties.getValue(it) }))
        }
        return result
    }

    /** Calls [block] with each [size]-element subset of [items], in order of the earliest items first. */
    private fun combinations(items: List<Int>, size: Int, limit: Int, block: (Set<Int>) -> Unit) {
        var produced = 0
        val chosen = ArrayList<Int>(size)
        fun recurse(from: Int) {
            if (produced >= limit) return
            if (chosen.size == size) {
                produced++
                block(chosen.toSet())
                return
            }
            for (i in from..items.size - (size - chosen.size)) {
                chosen += items[i]
                recurse(i + 1)
                chosen.removeAt(chosen.size - 1)
                if (produced >= limit) return
            }
        }
        recurse(0)
    }

    /**
     * How much it costs to leave each note out, in multiples of the drop weight: an inner octave doubling is the
     * cheapest to lose, then other inner voices, and the bass and the top note are the dearest.
     */
    private fun dropPenalties(ctx: DecodeContext, playable: List<Int>): Map<Int, Double> = playable.associateWith { note ->
        val outer = note == playable.first() || note == playable.last()
        val pc = ctx.notes[note].pitch.mod(12)
        val doubled = playable.any { it != note && ctx.notes[it].pitch.mod(12) == pc }
        when {
            outer -> OUTER_DROP
            doubled -> 1.0
            else -> INNER_DROP
        }
    }

    /** Among notes equally expendable, prefer to drop those nearest the middle of the chord. */
    private fun dropPriority(ctx: DecodeContext, playable: List<Int>, note: Int): Double {
        val middle = (ctx.notes[playable.first()].pitch + ctx.notes[playable.last()].pitch) / 2.0
        return kotlin.math.abs(ctx.notes[note].pitch - middle)
    }

    private fun empty(dropped: List<Int>, penalty: Double) = Candidate(
        id = 0, notes = IntArray(0), strings = IntArray(0), frets = IntArray(0), harmonic = BooleanArray(0),
        dropped = dropped.toIntArray(), dropPenalty = penalty, minFret = -1, maxFret = -1, barreFret = -1,
        barreWidth = 0, fingerCount = 0, spanMm = 0.0,
    )

    private fun positionsOf(ctx: DecodeContext, pitch: Int): List<Position> {
        val profile = ctx.profile
        val result = mutableListOf<Position>()
        for (string in 0 until profile.stringCount) {
            val open = ctx.tuning[string] + ctx.capo
            val relative = pitch - open
            if (relative >= 0 && ctx.capo + relative <= profile.fretCount) {
                result += Position(string, ctx.capo + relative, false)
            }
            if (profile.harmonics) {
                HARMONIC_NODES.forEach { (interval, node) ->
                    if (relative == interval && ctx.capo + node <= profile.fretCount) {
                        result += Position(string, ctx.capo + node, true)
                    }
                }
            }
        }
        return result
    }

    private fun enumerate(
        ctx: DecodeContext,
        kept: List<Int>,
        positions: Map<Int, List<Position>>,
        dropped: IntArray,
        dropPenalty: Double,
    ): List<Candidate> {
        if (kept.isEmpty()) return emptyList()
        val profile = ctx.profile
        val choice = arrayOfNulls<Position>(kept.size)
        val results = mutableListOf<Candidate>()
        var leaves = 0

        fun recurse(depth: Int, usedStrings: Int, minFretted: Int, maxFretted: Int) {
            if (leaves >= MAX_LEAVES) return
            if (depth == kept.size) {
                leaves++
                build(ctx, kept, choice.requireNoNulls(), dropped, dropPenalty)?.let { results += it }
                return
            }
            for (position in positions.getValue(kept[depth])) {
                if (usedStrings and (1 shl position.string) != 0) continue
                val fretted = position.fret > ctx.capo || position.harmonic
                val newMin = if (fretted) minOf(minFretted, position.fret) else minFretted
                val newMax = if (fretted) maxOf(maxFretted, position.fret) else maxFretted
                if (newMax >= 0 && newMin != Int.MAX_VALUE && profile.geometry.span(newMin, newMax) > profile.maxSpanMm) continue
                choice[depth] = position
                recurse(depth + 1, usedStrings or (1 shl position.string), newMin, newMax)
            }
        }
        recurse(0, 0, Int.MAX_VALUE, -1)
        return results
    }

    private fun build(
        ctx: DecodeContext,
        kept: List<Int>,
        choice: Array<Position>,
        dropped: IntArray,
        dropPenalty: Double,
    ): Candidate? {
        val profile = ctx.profile
        val strings = IntArray(kept.size) { choice[it].string }
        if (profile.bowed && strings.size > 1) {
            val sorted = strings.sorted()
            if (sorted.last() - sorted.first() != sorted.size - 1) return null
        }
        val frets = IntArray(kept.size) { choice[it].fret }
        val harmonic = BooleanArray(kept.size) { choice[it].harmonic }
        val fretted = kept.indices.filter { frets[it] > ctx.capo || harmonic[it] }

        var minFret = -1
        var maxFret = -1
        var barreFret = -1
        var barreWidth = 0
        var fingers = 0
        var span = 0.0
        if (fretted.isNotEmpty()) {
            minFret = fretted.minOf { frets[it] }
            maxFret = fretted.maxOf { frets[it] }
            span = profile.geometry.span(minFret, maxFret)
            val atMin = fretted.filter { frets[it] == minFret }
            val barreLow = atMin.minOf { strings[it] }
            val barreHigh = atMin.maxOf { strings[it] }
            // A barre can't cover a string that is meant to ring open.
            val barrePossible = atMin.size >= 2 && kept.indices.none {
                !fretted.contains(it) && strings[it] in barreLow..barreHigh
            }
            val withoutBarre = fretted.size
            val withBarre = 1 + fretted.count { frets[it] > minFret }
            val doubleStop = atMin.size == 2 && fretted.size == 2 && barreHigh - barreLow == 1
            val useBarre = barrePossible && (withoutBarre > MAX_FINGERS || atMin.size >= 3 || doubleStop)
            fingers = if (useBarre) withBarre else withoutBarre
            if (fingers > MAX_FINGERS) return null
            if (useBarre) {
                barreFret = minFret
                barreWidth = barreHigh - barreLow + 1
            }
        }
        return Candidate(
            id = 0, notes = kept.toIntArray(), strings = strings, frets = frets, harmonic = harmonic,
            dropped = dropped, dropPenalty = dropPenalty, minFret = minFret, maxFret = maxFret, barreFret = barreFret, barreWidth = barreWidth,
            fingerCount = fingers, spanMm = span,
        )
    }

    /**
     * Which finger presses each note of [candidate] when the index finger is on [indexFret]: `0` for open strings,
     * otherwise one finger per fret from the index, never reusing a finger except across a barre.
     */
    fun assignFingers(candidate: Candidate, indexFret: Int, capo: Int): IntArray {
        val fingers = IntArray(candidate.notes.size)
        val order = candidate.notes.indices
            .filter { candidate.frets[it] > capo || candidate.harmonic[it] }
            .sortedWith(compareBy({ candidate.frets[it] }, { candidate.strings[it] }))
        var next = 1
        for (k in order) {
            if (candidate.barreFret >= 0 && candidate.frets[k] == candidate.barreFret) {
                fingers[k] = 1
                next = 2
                continue
            }
            val finger = maxOf(next, 1 + candidate.frets[k] - indexFret).coerceIn(1, MAX_FINGERS)
            fingers[k] = finger
            next = finger + 1
        }
        return fingers
    }

    /** Natural harmonics: the interval above the open string, and the fret it is touched over. */
    private val HARMONIC_NODES = listOf(12 to 12, 19 to 7, 24 to 5)
}
