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

package org.wysko.midis2jam2.manager.camera.cinematic.analysis

/** The fewest beats a solo or duet must last to be filmed as one. */
private const val MIN_FEATURE_BEATS = 4

/** How many bars a part must rest before its return counts as an entrance worth cutting to. */
private const val ENTRANCE_REST_BARS = 4

/** How far above its own average interest a part must be to count as leading the band. */
private const val SOLO_MARGIN = 0.08f

/** What makes a moment worth filming. */
enum class MomentKind(val priority: Int) {
    /** One part stands clearly above the rest. */
    Solo(3),

    /** A part comes in after a long rest. */
    Entrance(4),

    /** The drummer breaks from the groove. */
    Fill(5),

    /** Two parts share the spotlight. */
    Duet(2),

    /** The whole band at full strength. */
    Tutti(1),

    /** Most of the band drops out. */
    Breakdown(1),

    /** A sudden accent from the whole band. */
    Hit(5),
}

/**
 * A stretch of the song worth pointing the camera at.
 *
 * @property start When the moment begins, in seconds.
 * @property end When the moment ends, in seconds.
 * @property subjects The subjects the moment is about, most important first.
 * @property kind What makes the moment worth filming.
 * @property strength How strongly the moment stands out, roughly 0–1.
 */
data class Moment(
    val start: Double,
    val end: Double,
    val subjects: List<Int>,
    val kind: MomentKind,
    val strength: Float,
) {
    override fun toString(): String = "$kind$subjects @ ${"%.2f".format(start)}–${"%.2f".format(end)}"
}

/**
 * Picks out the moments of a song worth filming, from the interest of every part and the song's sections.
 */
object MomentFinder {

    /** Finds every moment in the song, sorted by start time. */
    fun find(
        grid: BeatGrid,
        features: Map<Int, SubjectFeatures>,
        energy: FloatArray,
        sections: List<Section>,
    ): List<Moment> = buildList {
        addAll(features(grid, features))
        addAll(entrances(grid, features))
        addAll(fills(grid, features))
        addAll(hits(grid, features, energy))
        sections.forEach { section ->
            val ranked = rankByInterest(grid, features, section.start, section.end)
            when (section.role) {
                SectionRole.Peak -> if (ranked.size >= 3) {
                    add(Moment(section.start, section.end, ranked.take(4), MomentKind.Tutti, section.energy))
                }

                SectionRole.Breakdown -> if (ranked.isNotEmpty()) {
                    add(Moment(section.start, section.end, ranked.take(2), MomentKind.Breakdown, 1f - section.energy))
                }

                else -> Unit
            }
        }
    }.sortedWith(compareBy({ it.start }, { -it.kind.priority }))

    /** Subjects that play during [from] to [to], most interesting first. */
    fun rankByInterest(grid: BeatGrid, features: Map<Int, SubjectFeatures>, from: Double, to: Double): List<Int> {
        val beats = grid.beatAt(from)..grid.beatAt(to - 1e-6).coerceAtLeast(grid.beatAt(from))
        return features.values
            .map { f -> f.subject.id to beats.sumOf { f.interest[it].toDouble() } }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Solos and duets: stretches where one or two parts lead the band. Judged by how much parts stand out, not by how
     * fresh they are to watch, so a long solo is found to last as long as it does.
     */
    private fun features(grid: BeatGrid, features: Map<Int, SubjectFeatures>): List<Moment> {
        val all = features.values.toList()
        if (all.isEmpty()) return emptyList()

        // A part that is always this prominent is the backbone of the song, not a soloist. To lead, a part must
        // stand above its own average across the song.
        val baseline = all.associateWith { it.prominence.average().toFloat() }
        fun SubjectFeatures.standsOut(b: Int, margin: Float) = prominence[b] >= baseline.getValue(this) + margin

        // Who leads each beat: a single soloist, a pair, or nobody.
        val leaders = Array(grid.beatCount) { b ->
            val ranked = all.filter { it.prominence[b] > 0f }.sortedByDescending { it.prominence[b] }
            val first = ranked.getOrNull(0) ?: return@Array emptyList<Int>()
            val second = ranked.getOrNull(1)
            val firstProminence = first.prominence[b]
            val secondProminence = second?.prominence?.get(b) ?: 0f
            when {
                firstProminence >= 0.5f && first.standsOut(b, SOLO_MARGIN) &&
                    (first.share[b] >= 0.4f || firstProminence - secondProminence >= 0.2f) -> listOf(first.subject.id)

                second != null && secondProminence >= 0.45f && firstProminence - secondProminence < 0.15f &&
                    first.share[b] + second.share[b] >= 0.55f &&
                    (first.standsOut(b, SOLO_MARGIN) || second.standsOut(b, SOLO_MARGIN)) ->
                    listOf(first.subject.id, second.subject.id).sorted()

                else -> emptyList()
            }
        }

        val moments = mutableListOf<Moment>()
        var b = 0
        while (b < grid.beatCount) {
            val lead = leaders[b]
            if (lead.isEmpty()) {
                b++
                continue
            }
            var end = b
            var gap = 0
            var probe = b + 1
            // Allow a single beat's breath without ending the run.
            while (probe < grid.beatCount && gap <= 1) {
                if (leaders[probe] == lead) {
                    end = probe
                    gap = 0
                } else {
                    gap++
                }
                probe++
            }
            if (end - b + 1 >= MIN_FEATURE_BEATS) {
                val strength = lead.map { id -> (b..end).map { features.getValue(id).prominence[it] }.average() }
                    .average().toFloat()
                val kind = if (lead.size == 1) MomentKind.Solo else MomentKind.Duet
                moments += Moment(grid.timeOf(b), grid.timeOf(end + 1), lead, kind, strength)
            }
            b = end + 1
        }
        return moments
    }

    /**
     * Parts coming in after a long rest, but not the band starting the song together. A part that sat out only a bar
     * or two is still part of the texture, so it must have rested for [ENTRANCE_REST_BARS] bars.
     */
    private fun entrances(grid: BeatGrid, features: Map<Int, SubjectFeatures>): List<Moment> {
        val firstBeat = features.values
            .map { f -> f.onsets.indexOfFirst { it > 0 } }
            .filter { it >= 0 }
            .minOrNull() ?: return emptyList()
        return features.values.filter { it.echoOf == null }.flatMap { f ->
            f.entrance.indices
                .filter { b -> f.entrance[b] >= 1f && b >= firstBeat + grid.beatsPerBarAt(b) }
                .filter { b ->
                    val rest = ENTRANCE_REST_BARS * grid.beatsPerBarAt(b)
                    b >= rest && (b - rest until b).none { f.sounding[it] }
                }
                .map { b ->
                    val end = grid.timeOf(b + grid.beatsPerBarAt(b))
                    Moment(grid.timeOf(b), end, listOf(f.subject.id), MomentKind.Entrance, 0.6f + 0.4f * f.rarity)
                }
        }.let { arrivals(grid, it) }
    }

    /**
     * Entrances within a beat of each other, as one: when a band comes in together, the shot is of all of them, not
     * of whichever happened to be counted first. The strongest newcomer is listed first.
     */
    private fun arrivals(grid: BeatGrid, entrances: List<Moment>): List<Moment> {
        val merged = mutableListOf<MutableList<Moment>>()
        entrances.sortedBy { it.start }.forEach { entrance ->
            val group = merged.lastOrNull()
            val beat = grid.secondsPerBeatAt(grid.beatAt(entrance.start))
            if (group != null && entrance.start - group.first().start <= beat + 1e-6) group += entrance
            else merged += mutableListOf(entrance)
        }
        return merged.map { group ->
            val strongestFirst = group.sortedByDescending { it.strength }
            Moment(
                group.minOf { it.start },
                group.maxOf { it.end },
                strongestFirst.flatMap { it.subjects }.distinct(),
                MomentKind.Entrance,
                strongestFirst.first().strength,
            )
        }
    }

    /** Drum fills: runs of beats that break from the groove. */
    private fun fills(grid: BeatGrid, features: Map<Int, SubjectFeatures>): List<Moment> =
        features.values.filter { it.subject.kind == SubjectKind.Drums }.flatMap { f ->
            val moments = mutableListOf<Moment>()
            var b = 0
            while (b < grid.beatCount) {
                if (f.fill[b] < 0.4f) {
                    b++
                    continue
                }
                var end = b
                while (end + 1 < grid.beatCount && f.fill[end + 1] >= 0.4f) end++
                val strength = (b..end).maxOf { f.fill[it] }
                moments += Moment(grid.timeOf(b), grid.timeOf(end + 1), listOf(f.subject.id), MomentKind.Fill, strength)
                b = end + 1
            }
            moments
        }

    /** Sudden accents: a beat far louder than the beats before it, with much of the band playing on it. */
    private fun hits(grid: BeatGrid, features: Map<Int, SubjectFeatures>, energy: FloatArray): List<Moment> {
        val moments = mutableListOf<Moment>()
        for (b in 4 until grid.beatCount) {
            val before = (b - 4 until b).map { energy[it] }.average().toFloat()
            if (energy[b] < 0.5f || energy[b] < before * 1.8f + 0.1f) continue
            val playing = features.values.filter { it.onsets[b] > 0 }
            val crash = features.values.any { it.crash[b] }
            if (playing.size < 3 && !crash) continue
            val subjects = playing.sortedByDescending { it.interest[b] }.map { it.subject.id }
            moments += Moment(grid.timeOf(b), grid.timeOf(b + 1), subjects, MomentKind.Hit, energy[b] - before)
        }
        return moments
    }
}
