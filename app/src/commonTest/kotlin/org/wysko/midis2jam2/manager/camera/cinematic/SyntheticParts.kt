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

package org.wysko.midis2jam2.manager.camera.cinematic

import org.wysko.midis2jam2.manager.camera.cinematic.analysis.BeatGrid
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.NoteSample
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectNotes

/**
 * Parts written beat by beat for the cinematic camera's tests, on a steady 120 BPM grid in 4/4.
 */
internal object SyntheticParts {
    const val SECONDS_PER_BEAT = 0.5

    fun grid(beats: Int): BeatGrid = BeatGrid.uniform(SECONDS_PER_BEAT, 4, beats * SECONDS_PER_BEAT)

    fun time(beat: Int): Double = beat * SECONDS_PER_BEAT

    /** A busy, wandering melody: eighth notes over [beats]. */
    fun melody(id: Int, beats: IntRange, kind: SubjectKind = SubjectKind.Lead, velocity: Int = 100): SubjectNotes =
        SubjectNotes(
            id,
            kind,
            beats.flatMap { b ->
                (0..1).map { half ->
                    val start = time(b) + half * SECONDS_PER_BEAT / 2
                    val pitch = 60 + listOf(0, 4, 7, 12, 9, 5, 2, 11)[(b * 2 + half) % 8]
                    NoteSample(start, start + SECONDS_PER_BEAT / 2, pitch, velocity)
                }
            },
        )

    /** Chords held for [length] beats at a time across [beats]. */
    fun pads(id: Int, beats: IntRange, length: Int, kind: SubjectKind = SubjectKind.Ensemble): SubjectNotes =
        SubjectNotes(
            id,
            kind,
            beats.step(length).flatMap { b ->
                listOf(48, 52, 55).map { NoteSample(time(b), time(b + length), it, 80) }
            },
        )

    /** One quarter-note chord per beat across [beats]. */
    fun comping(id: Int, beats: IntRange, kind: SubjectKind = SubjectKind.Keys): SubjectNotes =
        SubjectNotes(
            id,
            kind,
            beats.flatMap { b -> listOf(48, 52, 55).map { NoteSample(time(b), time(b + 1), it, 80) } },
        )

    /**
     * A rock groove — kick on 1 and 3, snare on 2 and 4, eighth-note hi-hats — across [beats], with four tom hits
     * on each beat in [fillBeats] instead.
     */
    fun drums(id: Int, beats: IntRange, fillBeats: Set<Int> = emptySet()): SubjectNotes =
        SubjectNotes(
            id,
            SubjectKind.Drums,
            beats.flatMap { b ->
                if (b in fillBeats) {
                    listOf(50, 48, 45, 43).mapIndexed { i, tom ->
                        val start = time(b) + i * SECONDS_PER_BEAT / 4
                        NoteSample(start, start + 0.1, tom, 110)
                    }
                } else {
                    val main = if (b % 2 == 0) 36 else 38
                    listOf(
                        NoteSample(time(b), time(b) + 0.1, main, 100),
                        NoteSample(time(b), time(b) + 0.1, 42, 80),
                        NoteSample(time(b) + SECONDS_PER_BEAT / 2, time(b) + SECONDS_PER_BEAT / 2 + 0.1, 42, 70),
                    )
                }
            },
            isStruck = true,
        )
}
