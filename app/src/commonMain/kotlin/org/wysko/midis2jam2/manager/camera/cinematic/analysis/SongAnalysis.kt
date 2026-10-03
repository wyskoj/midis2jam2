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

/**
 * Everything the cinematic camera knows about a song before it starts filming.
 *
 * @property grid The beats and bar lines.
 * @property subjects Every instrument's notes.
 * @property features What each instrument is doing on each beat, keyed by subject id.
 * @property energy How much the whole band is doing on each beat, relative to the busiest beat, 0–1.
 * @property visibility When each instrument is on stage, keyed by subject id.
 * @property sections The song's sections, in order.
 * @property moments The moments worth filming, sorted by start time.
 * @property musicStart When the first note begins, in seconds.
 * @property musicEnd When the last note ends, in seconds.
 */
class SongAnalysis(
    val grid: BeatGrid,
    val subjects: List<SubjectNotes>,
    val features: Map<Int, SubjectFeatures>,
    val energy: FloatArray,
    val visibility: Map<Int, StageVisibility>,
    val sections: List<Section>,
    val moments: List<Moment>,
    val musicStart: Double,
    val musicEnd: Double,
) {
    /** The section playing at [time], if any. */
    fun sectionAt(time: Double): Section? = sections.lastOrNull { it.start <= time } ?: sections.firstOrNull()

    /** The kind of subject [id] is. */
    fun kindOf(id: Int): SubjectKind = features[id]?.subject?.kind ?: SubjectKind.Other

    /** What instrument subject [id] is, or its id if that isn't known. Parts with the same name look alike. */
    fun nameOf(id: Int): String = features[id]?.subject?.name?.ifEmpty { null } ?: "#$id"

    /** Whether subject [id] is on stage at [time]. */
    fun isOnStageAt(id: Int, time: Double): Boolean = visibility[id]?.isVisibleAt(time) ?: false

    /**
     * Whether subject [id] is moved about the stage during [from] to [to]. Instruments of a kind stand in a row, and
     * shift along to make room whenever another of their kind comes on stage or leaves it. The shift takes a moment,
     * so changes a little before [from] count too.
     */
    fun movesDuring(id: Int, from: Double, to: Double): Boolean {
        val name = nameOf(id)
        val window = (from - SHIFT_SETTLES)..to
        return subjects.any { other ->
            other.id != id && nameOf(other.id) == name &&
                visibility[other.id]?.spans.orEmpty().any { it.start in window || it.endInclusive in window }
        }
    }

    /** Whether subject [id] plays any notes during [from] to [to]. */
    fun playsDuring(id: Int, from: Double, to: Double): Boolean {
        val f = features[id] ?: return false
        val first = grid.beatAt(from)
        val last = grid.beatAt(to - 1e-6).coerceAtLeast(first)
        return (first..last).any { f.onsets[it] > 0 }
    }

    /** Whether subject [id] is on stage for all of [from] to [to]. */
    fun isOnStageThroughout(id: Int, from: Double, to: Double): Boolean =
        visibility[id]?.isVisibleThroughout(from, to) ?: false

    /** The total interest of subject [id] over [from] to [to]. */
    /**
     * How interesting [id] is from [from] to [to], against the most interesting other player on stage throughout:
     * 1 when level, below 1 when someone else is more interesting. Infinite when no one else is playing.
     */
    fun standingOf(id: Int, from: Double, to: Double): Float {
        val best = subjects.filter { it.id != id && isOnStageThroughout(it.id, from, to) }
            .maxOfOrNull { interestOver(it.id, from, to) } ?: 0f
        val own = interestOver(id, from, to)
        return if (best <= 0f) Float.POSITIVE_INFINITY else own / best
    }

    fun interestOver(id: Int, from: Double, to: Double): Float {
        val f = features[id] ?: return 0f
        val first = grid.beatAt(from)
        val last = grid.beatAt(to - 1e-6).coerceAtLeast(first)
        return (first..last).sumOf { f.interest[it].toDouble() }.toFloat()
    }

    companion object {
        /** How long instruments take to shift along the stage when another of their kind comes or goes, in seconds. */
        const val SHIFT_SETTLES: Double = 1.0

        /**
         * Analyses [subjects] against [grid].
         *
         * @param alwaysVisible Whether instruments are set to always show, so every one is always on stage.
         */
        fun of(subjects: List<SubjectNotes>, grid: BeatGrid, alwaysVisible: Boolean = false): SongAnalysis {
            val features = InterestAnalyzer.analyze(subjects, grid)
            val energy = ensembleEnergy(features.values, grid.beatCount)
            val sections = SectionDetector.detect(grid, energy, features.values)
            val moments = MomentFinder.find(grid, features, energy, sections)
            val visibility = subjects.associate {
                it.id to if (alwaysVisible) StageVisibility.ALWAYS else StageVisibility.of(it)
            }
            val notes = subjects.flatMap { it.notes }
            return SongAnalysis(
                grid = grid,
                subjects = subjects,
                features = features,
                energy = energy,
                visibility = visibility,
                sections = sections,
                moments = moments,
                musicStart = notes.minOfOrNull { it.start } ?: 0.0,
                musicEnd = notes.maxOfOrNull { it.end } ?: 0.0,
            )
        }

        /** How much the whole band is doing on each beat, normalised so the busiest beat is 1. */
        private fun ensembleEnergy(features: Collection<SubjectFeatures>, beatCount: Int): FloatArray {
            val energy = FloatArray(beatCount) { b ->
                features.sumOf { f ->
                    val loudness = if (f.onsets[b] > 0) f.velocity[b] / 127.0 else 0.5
                    f.density[b] * loudness + if (f.sounding[b]) 0.3 else 0.0
                }.toFloat()
            }
            val peak = energy.maxOrNull()?.takeIf { it > 0f } ?: return energy
            for (b in energy.indices) energy[b] /= peak
            return energy
        }
    }
}
