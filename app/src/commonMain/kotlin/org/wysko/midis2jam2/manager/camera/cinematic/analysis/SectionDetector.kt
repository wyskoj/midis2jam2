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

import kotlin.math.abs
import kotlin.math.sqrt

/** The fewest bars a section can last. */
private const val MIN_SECTION_BARS = 4

/** The least a bar must differ from the bars before it to start a new section, however steady the song. */
private const val MIN_BOUNDARY_NOVELTY = 0.15

/** Section energy, relative to the song's average, below which an opening or ending counts as quiet. */
private const val QUIET = 0.85f

/** Section energy, relative to the song's average, at or above which the band is at full strength. */
private const val LOUD = 1.2f

/** Section energy, relative to the song's average, at or below which most of the band has dropped out. */
private const val VERY_QUIET = 0.6f

/** What part a section plays in the shape of the song. */
enum class SectionRole {
    /** The opening, before the band is in full swing. */
    Intro,

    /** Rising towards a peak. */
    Build,

    /** The song at full strength. */
    Peak,

    /** Most of the band drops out. */
    Breakdown,

    /** An ordinary stretch of the song. */
    Groove,

    /** The ending. */
    Outro,
}

/**
 * A stretch of the song with a consistent energy and line-up.
 *
 * @property start When the section begins, in seconds.
 * @property end When the section ends, in seconds.
 * @property role What part the section plays in the song.
 * @property energy How much the band is doing, relative to the busiest bar of the song, 0–1.
 * @property active The subjects that play through most of the section.
 */
data class Section(
    val start: Double,
    val end: Double,
    val role: SectionRole,
    val energy: Float,
    val active: Set<Int>,
)

/**
 * Splits a song into sections where its energy or line-up changes, and labels each one.
 */
object SectionDetector {

    /**
     * Finds the sections of a song from the band's [energy] on each beat and the [features] of every subject.
     */
    fun detect(grid: BeatGrid, energy: FloatArray, features: Collection<SubjectFeatures>): List<Section> {
        val bars = grid.barStartBeats.ifEmpty { listOf(0) }
        val barRanges = bars.mapIndexed { i, start -> start until (bars.getOrNull(i + 1) ?: grid.beatCount) }
            .filter { !it.isEmpty() }
        if (barRanges.isEmpty()) return emptyList()

        val barEnergy = barRanges.map { range -> range.map { energy[it] }.average().toFloat() }
        val barActive = barRanges.map { range ->
            features.filter { f -> range.count { f.sounding[it] } * 4 >= range.count() }.map { it.subject.id }.toSet()
        }

        val songEnergy = barEnergy.average().toFloat().coerceAtLeast(1e-6f)

        // How much each bar differs from the two before it: in energy, relative to the song as a whole, and in
        // who is playing.
        val novelty = DoubleArray(barRanges.size) { i ->
            if (i == 0) return@DoubleArray 0.0
            val before = (i - 2).coerceAtLeast(0) until i
            val after = i until (i + 2).coerceAtMost(barRanges.size)
            val energyChange = abs(after.map { barEnergy[it] }.average() - before.map { barEnergy[it] }.average())
            val lineUpChange = jaccardDistance(
                before.flatMap { barActive[it] }.toSet(),
                after.flatMap { barActive[it] }.toSet(),
            )
            0.6 * (energyChange / songEnergy).coerceAtMost(1.0) + 0.4 * lineUpChange
        }

        // Boundaries fall on the strongest changes, measured against how much this song changes in general, and
        // never closer together than a few bars.
        val mean = novelty.average()
        val spread = sqrt(novelty.map { (it - mean) * (it - mean) }.average())
        val threshold = maxOf(MIN_BOUNDARY_NOVELTY, mean + 0.75 * spread)
        val boundaries = sortedSetOf(0)
        novelty.indices
            .filter { i -> novelty[i] >= threshold && i >= MIN_SECTION_BARS / 2 && barRanges.size - i >= MIN_SECTION_BARS / 2 }
            .sortedByDescending { novelty[it] }
            .forEach { i -> if (boundaries.none { abs(it - i) < MIN_SECTION_BARS }) boundaries += i }

        val ordered = boundaries.toList()
        val spans = ordered.mapIndexed { i, first -> first until (ordered.getOrNull(i + 1) ?: barRanges.size) }
        val peakEnergy = barEnergy.max().coerceAtLeast(1e-6f)
        val raw = spans.map { span ->
            val beats = barRanges[span.first].first until barRanges[span.last].last + 1
            val energy = span.map { barEnergy[it] }.average().toFloat()
            Triple(
                beats,
                energy,
                span.flatMap { barActive[it] }.groupingBy { it }.eachCount()
                    .filterValues { it * 2 >= span.count() }.keys,
            )
        }

        // Each section is judged against the song's own average, so one loud bar doesn't make the rest of the
        // song a breakdown.
        return raw.mapIndexed { i, (beats, energy, active) ->
            val ratio = energy / songEnergy
            val nextRatio = raw.getOrNull(i + 1)?.second?.div(songEnergy)
            val role = when {
                raw.size == 1 -> SectionRole.Groove
                i == 0 && ratio < QUIET -> SectionRole.Intro
                i == raw.lastIndex && ratio < QUIET -> SectionRole.Outro
                ratio >= LOUD -> SectionRole.Peak
                ratio <= VERY_QUIET -> SectionRole.Breakdown
                nextRatio != null && nextRatio >= LOUD && nextRatio > ratio -> SectionRole.Build
                else -> SectionRole.Groove
            }
            Section(grid.timeOf(beats.first), grid.timeOf(beats.last + 1), role, energy / peakEnergy, active)
        }
    }

    private fun jaccardDistance(a: Set<Int>, b: Set<Int>): Double {
        val union = (a + b).size
        if (union == 0) return 0.0
        return 1.0 - (a intersect b).size.toDouble() / union
    }
}
