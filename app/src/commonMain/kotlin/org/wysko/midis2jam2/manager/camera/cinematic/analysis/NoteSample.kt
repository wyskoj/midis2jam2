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
 * One note, reduced to what the cinematic camera needs to judge how interesting a part is.
 *
 * @property start When the note begins, in seconds from the start of the song.
 * @property end When the note ends, in seconds. Struck instruments use a short nominal decay.
 * @property note The MIDI note number.
 * @property velocity The MIDI velocity, 0–127.
 */
data class NoteSample(
    val start: Double,
    val end: Double,
    val note: Int,
    val velocity: Int,
)

/**
 * A coarse grouping of instruments by how they are filmed and how their parts are read.
 */
enum class SubjectKind {
    /** A drum kit. Its toms, snares and crashes are read for fills. */
    Drums,

    /** Auxiliary and melodic percussion struck one note at a time. */
    Percussion,

    /** Pianos, organs, mallets and other keyed instruments. */
    Keys,

    /** Guitars and other fretted instruments, except the bass. */
    Guitar,

    /** Bass guitars and the upright bass. */
    Bass,

    /** Wind and solo string instruments that usually carry a melody. */
    Lead,

    /** Large ensembles: stage strings, choirs, horn sections. */
    Ensemble,

    /** Anything else. */
    Other,
}

/**
 * Everything one instrument plays, as seen by the analysis.
 *
 * @property id The instrument's index in the performance's instrument list.
 * @property kind How the instrument is filmed.
 * @property notes Every note the instrument plays, sorted by [NoteSample.start].
 * @property isStruck Whether the instrument only reacts to note onsets, so its visibility counts from onsets
 * rather than note ends.
 * @property name What instrument it is, such as `Guitar`. Two parts with the same name look alike on screen.
 */
class SubjectNotes(
    val id: Int,
    val kind: SubjectKind,
    notes: List<NoteSample>,
    val isStruck: Boolean = false,
    val name: String = "",
) {
    val notes: List<NoteSample> = notes.sortedBy { it.start }

    override fun toString(): String = "Subject($id, $kind, ${notes.size} notes)"
}
