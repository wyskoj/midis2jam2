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
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/** Drum notes that move around the kit: the toms. */
private val TOMS = setOf(41, 43, 45, 47, 48, 50)

/** Drum notes on the snare. */
private val SNARES = setOf(37, 38, 40)

/** Drum notes on a crash or splash cymbal. */
private val CRASHES = setOf(49, 52, 55, 57)

/** How many beats either side of a beat count towards its note density. */
private const val DENSITY_HALF_WINDOW = 2

/** How many beats of silence before a part's return counts as an entrance. */
private const val ENTRANCE_SILENCE_BEATS = 8

/** How many beats of groove a fill is measured against. */
private const val FILL_BASELINE_BEATS = 8

/** How much of the attack and release of a part's interest carries from one beat to the next. */
private const val ATTACK = 0.75f
private const val RELEASE = 0.3f

/** How many beats back a part's playing is compared with, to judge whether it is doing something new. */
private const val NOVELTY_BEATS = 32

/**
 * How interesting a part is while it keeps doing what it has been doing, against when it does something new. The
 * camera has seen it: a keyboard comping the same way for a minute is less worth watching than a violin breaking
 * into a run.
 */
private const val HABITUATED = 0.6f

/** How interesting a part that only echoes another is, against what it would be on its own merits. */
private const val ECHO_WEIGHT = 0.15f

/**
 * What one instrument is doing on each beat of the song, and how interesting that is to watch.
 *
 * Every array has one entry per beat of the [BeatGrid].
 */
class SubjectFeatures(val subject: SubjectNotes, beatCount: Int) {
    /** Notes that begin on each beat. */
    val onsets: IntArray = IntArray(beatCount)

    /** Whether any note is sounding during each beat. */
    val sounding: BooleanArray = BooleanArray(beatCount)

    /** The mean velocity of the notes that begin on each beat, 0–127, or 0 if none do. */
    val velocity: FloatArray = FloatArray(beatCount)

    /** Notes per second around each beat, over a short window. */
    val density: FloatArray = FloatArray(beatCount)

    /** How much the melody moves on each beat, 0–1. Always 0 for unpitched parts. */
    val melodic: FloatArray = FloatArray(beatCount)

    /** The mean pitch of the notes that begin on each beat, or 0 if none do. */
    internal val meanPitch: FloatArray = FloatArray(beatCount)

    /** Whether the part plays one note at a time on each beat, rather than chords. */
    internal val singleLine: BooleanArray = BooleanArray(beatCount) { true }

    /** Whether this part is the highest single line among the pitched parts playing on each beat. */
    val topVoice: BooleanArray = BooleanArray(beatCount)

    /** A spike when the part comes in after a long rest, decaying over the next few beats, 0–1. */
    val entrance: FloatArray = FloatArray(beatCount)

    /** For drum kits, how much each beat stands out from the groove as a fill, 0–1. */
    val fill: FloatArray = FloatArray(beatCount)

    /** For drum kits, whether a crash cymbal is struck on each beat. */
    val crash: BooleanArray = BooleanArray(beatCount)

    /** This part's share of everything the band is doing on each beat, 0–1. */
    val share: FloatArray = FloatArray(beatCount)

    /** How rarely the part plays across the song: 1 for a part heard once, 0 for one that never stops. */
    var rarity: Float = 0f
        internal set

    /** The part this one only echoes (see [Echoes]), if it does. */
    var echoOf: Int? = null
        internal set

    /** How interesting the part is to watch on each beat, smoothed. Roughly 0–1, higher during fills and entrances. */
    val interest: FloatArray = FloatArray(beatCount)

    /**
     * How much the part stands out on each beat, smoothed: its [interest] before the camera tires of it. A soloist
     * stays prominent for the whole solo, though after a while they are less of a surprise to watch.
     */
    val prominence: FloatArray = FloatArray(beatCount)

    override fun toString(): String = "Features(${subject.id})"
}

/**
 * Scores every instrument, on every beat, for how interesting it is to film.
 *
 * Interest rewards parts that are busy, loud for themselves, melodically active, carrying the top line, or carrying
 * a large share of the band. It spikes when a part comes in after a long rest and when a drummer plays a fill, and
 * it is held down for long sustained notes that give the camera nothing to watch.
 */
object InterestAnalyzer {

    /** Analyses every subject against [grid]. The result is keyed by [SubjectNotes.id]. */
    fun analyze(subjects: List<SubjectNotes>, grid: BeatGrid): Map<Int, SubjectFeatures> {
        val features = subjects.associate { it.id to SubjectFeatures(it, grid.beatCount) }
        Echoes.find(subjects).forEach { (echo, original) -> features.getValue(echo).echoOf = original }
        val span = musicalSpan(subjects, grid)

        features.values.forEach { measure(it, grid, span) }
        markTopVoices(features.values, grid.beatCount)
        measureShares(features.values, grid.beatCount)
        features.values.forEach { score(it, grid) }

        return features
    }

    /** The first and last beats that have any music, or the whole grid if there is none. */
    private fun musicalSpan(subjects: List<SubjectNotes>, grid: BeatGrid): IntRange {
        val notes = subjects.flatMap { it.notes }
        if (notes.isEmpty()) return 0 until grid.beatCount
        return grid.beatAt(notes.minOf { it.start })..grid.beatAt(notes.maxOf { it.end })
    }

    private fun measure(features: SubjectFeatures, grid: BeatGrid, span: IntRange) = with(features) {
        val beatCount = grid.beatCount
        val velocitySums = FloatArray(beatCount)
        val tomCounts = FloatArray(beatCount)
        val attacks = IntArray(beatCount)

        subject.notes.forEach { note ->
            val beat = grid.beatAt(note.start)
            onsets[beat]++
            velocitySums[beat] += note.velocity.toFloat()
            val lastBeat = grid.beatAt((note.end - 1e-6).coerceAtLeast(note.start))
            for (b in beat..lastBeat) sounding[b] = true

            if (subject.kind == SubjectKind.Drums) {
                when (note.note) {
                    in TOMS -> tomCounts[beat] += 1f
                    in SNARES -> tomCounts[beat] += 0.5f
                    in CRASHES -> crash[beat] = true
                }
            }
        }

        for (b in 0 until beatCount) {
            if (onsets[b] > 0) velocity[b] = velocitySums[b] / onsets[b]
        }

        // Notes that start together are one attack: a chord, or a kick and hi-hat struck at once. Only the top note
        // of each attack counts towards the melodic line.
        val topLineByBeat = Array(beatCount) { mutableListOf<Int>() }
        subject.notes.groupBy { it.start }.forEach { (start, together) ->
            val beat = grid.beatAt(start)
            attacks[beat]++
            topLineByBeat[beat] += together.maxOf { it.note }
            if (together.size > 1) singleLine[beat] = false
        }

        // Density over a short window, so a part's busyness doesn't flicker from beat to beat.
        val prefix = IntArray(beatCount + 1)
        for (b in 0 until beatCount) prefix[b + 1] = prefix[b] + attacks[b]
        for (b in 0 until beatCount) {
            val from = (b - DENSITY_HALF_WINDOW).coerceAtLeast(0)
            val to = (b + DENSITY_HALF_WINDOW).coerceAtMost(beatCount)
            val seconds = (grid.timeOf(to) - grid.timeOf(from)).coerceAtLeast(1e-3)
            density[b] = ((prefix[to] - prefix[from]) / seconds).toFloat()
        }

        if (subject.kind.isPitched) {
            var previousTop: Int? = null
            for (b in 0 until beatCount) {
                val line = topLineByBeat[b]
                if (line.isEmpty()) continue
                meanPitch[b] = line.average().toFloat()
                // The melody's movement includes the step in from the previous beat's last note.
                val steps = (listOfNotNull(previousTop) + line).zipWithNext { a, c -> abs(c - a) }
                previousTop = line.last()
                if (steps.isEmpty()) continue
                val motion = steps.average().toFloat()
                val range = (line.max() - line.min()).toFloat()
                melodic[b] = 0.6f * (motion / 5f).coerceAtMost(1f) + 0.4f * (range / 12f).coerceAtMost(1f)
            }
        }

        // An entrance is a return after a long rest, or the first notes of a part that doesn't start the song.
        var silentFor = ENTRANCE_SILENCE_BEATS
        for (b in 0 until beatCount) {
            if (onsets[b] > 0 && silentFor >= ENTRANCE_SILENCE_BEATS) {
                for (k in 0..3) if (b + k < beatCount) {
                    entrance[b + k] = maxOf(entrance[b + k], 0.7f.pow(k))
                }
            }
            silentFor = if (sounding[b]) 0 else silentFor + 1
        }

        if (subject.kind == SubjectKind.Drums) {
            for (b in 0 until beatCount) {
                val from = (b - FILL_BASELINE_BEATS).coerceAtLeast(0)
                val baseline = if (b > from) (from until b).sumOf { tomCounts[it].toDouble() } / (b - from) else 0.0
                val excess = tomCounts[b] - baseline - 0.75
                fill[b] = (excess / 2.5).toFloat().coerceIn(0f, 1f)
            }
        }

        val playingBeats = span.count { it in 0 until beatCount && sounding[it] }
        rarity = 1f - playingBeats.toFloat() / span.count().coerceAtLeast(1)
    }

    /** Marks, on each beat, the pitched part playing the highest single line. */
    private fun markTopVoices(all: Collection<SubjectFeatures>, beatCount: Int) {
        // An echo plays the top line over again; the part it echoes is the one carrying it.
        val pitched = all.filter { it.subject.kind.isPitched && it.echoOf == null }
        for (b in 0 until beatCount) {
            pitched.filter { it.onsets[b] > 0 && it.singleLine[b] }
                .maxByOrNull { it.meanPitch[b] }
                ?.topVoice?.set(b, true)
        }
    }

    /** Works out each part's share of what the whole band is doing, beat by beat. */
    private fun measureShares(all: Collection<SubjectFeatures>, beatCount: Int) {
        for (b in 0 until beatCount) {
            // An echo adds nothing of its own to what the band is doing.
            val activities = all.associateWith { if (it.echoOf == null) it.activity(b) else 0f }
            val total = activities.values.sum()
            if (total <= 0f) continue
            activities.forEach { (features, activity) -> features.share[b] = activity / total }
        }
    }

    /**
     * How much a part is doing on [beat], weighted so that the drummer's steady groove doesn't drown everyone else.
     */
    private fun SubjectFeatures.activity(beat: Int): Float {
        val loudness = if (onsets[beat] > 0) velocity[beat] / 127f else 0.5f
        val sustain = if (sounding[beat]) 0.4f else 0f
        return (density[beat] * loudness + sustain) * subject.kind.activityWeight
    }

    private fun score(features: SubjectFeatures, grid: BeatGrid) = with(features) {
        val playedVelocities = subject.notes.map { it.velocity.toDouble() }
        val meanVelocity = playedVelocities.average().takeIf { !it.isNaN() } ?: 64.0
        val velocitySpread = sqrt(playedVelocities.map { (it - meanVelocity).pow(2) }.average())
            .takeIf { !it.isNaN() && it > 1.0 } ?: 12.0
        val medianDensity = density.filterIndexed { b, _ -> onsets[b] > 0 }.sorted()
            .let { if (it.isEmpty()) 1f else it[it.size / 2] }.coerceAtLeast(0.25f)
        val meanNoteBeats = subject.notes.map { (it.end - it.start) }.average()
            .takeIf { !it.isNaN() }?.let { it / grid.secondsPerBeatAt(0) } ?: 1.0

        var smoothed = 0f
        var smoothedProminence = 0f
        val base = FloatArray(interest.size)
        val steady = FloatArray(interest.size)
        var trailingSum = 0f
        for (b in interest.indices) {
            val raw = if (!sounding[b] && onsets[b] == 0) {
                0f
            } else {
                val ownDensity = 1f - exp(-density[b] / medianDensity)
                val absoluteDensity = 1f - exp(-density[b] / 4f)
                val densityScore = 0.5f * ownDensity + 0.5f * absoluteDensity
                val velocityScore = if (onsets[b] > 0) {
                    (((velocity[b] - meanVelocity) / velocitySpread).coerceIn(-2.0, 2.0) + 2.0).toFloat() / 4f
                } else {
                    0.5f
                }
                val top = if (topVoice[b]) 1f else 0f

                var value = (
                    0.30f * densityScore +
                        0.12f * velocityScore +
                        0.15f * melodic[b] +
                        0.10f * top +
                        0.25f * share[b] +
                        0.08f * rarity
                    ) * subject.kind.grooveWeight

                // Long held notes with little happening give the camera nothing to watch.
                if (meanNoteBeats >= 2.0 && density[b] < 1f) value *= 0.45f
                base[b] = value
                steady[b] = (value + 0.6f * entrance[b] + 0.8f * fill[b]) * if (echoOf != null) ECHO_WEIGHT else 1f

                // Doing more than it has been lately is worth watching; carrying on the same way is less so.
                val trailing = trailingSum / NOVELTY_BEATS
                val novelty = ((value - trailing) / value.coerceAtLeast(0.05f)).coerceIn(0f, 1f)
                value *= HABITUATED + (1f - HABITUATED) * novelty

                value += 0.6f * entrance[b] + 0.8f * fill[b]
                if (echoOf != null) value *= ECHO_WEIGHT
                value
            }
            trailingSum += base[b] - if (b >= NOVELTY_BEATS) base[b - NOVELTY_BEATS] else 0f
            smoothed += (raw - smoothed) * if (raw > smoothed) ATTACK else RELEASE
            interest[b] = smoothed
            smoothedProminence += (steady[b] - smoothedProminence) *
                if (steady[b] > smoothedProminence) ATTACK else RELEASE
            prominence[b] = smoothedProminence
        }
    }

    /** Whether parts of this kind play pitches that form a melody. */
    val SubjectKind.isPitched: Boolean
        get() = this != SubjectKind.Drums && this != SubjectKind.Percussion

    /**
     * How interesting a part of this kind is when it is simply keeping time. A drummer's groove is busy, but it's the
     * fills that are worth watching.
     */
    private val SubjectKind.grooveWeight: Float
        get() = when (this) {
            SubjectKind.Drums -> 0.7f
            SubjectKind.Percussion -> 0.6f
            else -> 1f
        }

    /** How much a part of this kind counts towards the band's activity. */
    private val SubjectKind.activityWeight: Float
        get() = when (this) {
            SubjectKind.Drums -> 0.35f
            SubjectKind.Percussion -> 0.5f
            SubjectKind.Ensemble -> 0.7f
            else -> 1f
        }
}
