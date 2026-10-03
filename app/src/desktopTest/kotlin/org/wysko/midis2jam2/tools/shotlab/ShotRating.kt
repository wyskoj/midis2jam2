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

package org.wysko.midis2jam2.tools.shotlab

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * One instrument in a rated shot.
 *
 * @property id Its index in the performance's instrument list.
 * @property instrument Its class, such as `AltoSax`.
 * @property kind How the cinematic camera classes it, such as `Lead`.
 */
@Serializable
data class RatedSubject(val id: Int, val instrument: String, val kind: String)

/** The shot before the rated one, for judging the cut into it. */
@Serializable
data class PreviousShot(
    val size: String,
    val move: String,
    val subjects: List<RatedSubject>,
    val reason: String,
    val yaw: Float,
    val pitch: Float,
)

/**
 * A shot as the cinematic camera planned and filmed it, and what the viewer thought of it.
 *
 * Everything the camera decided is recorded alongside the rating, so ratings can be compared across shot sizes,
 * moves, lenses, angles, instruments and the reasons the shot was chosen.
 */
@Serializable
data class ShotRating(
    val ratedAt: String,
    val song: String,
    val seed: Long,
    val shotIndex: Int,
    val shotCount: Int,
    val start: Double,
    val length: Double,
    val reason: String,
    val section: String?,
    val size: String,
    val move: String,
    val lens: String,
    val subjects: List<RatedSubject>,
    val plannedYaw: Float,
    val plannedPitch: Float,
    val filmedYaw: Float,
    val filmedPitch: Float,
    val clearView: Float,
    val framingEverything: Boolean,
    val fieldOfView: Float,
    val pacing: String,
    val handheld: Boolean,
    val previous: PreviousShot?,
    val stars: Int,
    val tags: List<String>,
    val comment: String,
    /** Where the song was played from, so the shot can be found again. */
    val songPath: String = "",
    /** Whether the angle is based on the subject's preferred view. */
    val usesPreferredView: Boolean = false,
    /** Where the camera stood as the shot began, and the box it framed. */
    val cameraLocation: List<Float> = emptyList(),
    val boxCenter: List<Float> = emptyList(),
    val boxExtent: List<Float> = emptyList(),
    /** How the framed box was turned, as a quaternion (x, y, z, w); its extent is along its own axes. */
    val boxRotation: List<Float> = emptyList(),
    /** How the camera actually moved, if the planned move was set aside. */
    val filmedMove: String = "",
    /** Whether the camera whipped round into the shot rather than cutting. */
    val whipped: Boolean = false,
)

/**
 * What identifies a shot across sessions: the song, the edit, where the shot falls in it and what it films. If the
 * planner changes, a shot that used to be at this place in this edit won't match, and can be rated afresh.
 */
fun shotKey(
    song: String,
    seed: Long,
    shotIndex: Int,
    start: Double,
    size: String,
    move: String,
    subjects: List<Int>,
): String = "$song|$seed|$shotIndex|${"%.3f".format(java.util.Locale.ROOT, start)}|$size|$move|${subjects.joinToString(",")}"

/** What identifies the shot this rating is of. */
val ShotRating.key: String
    get() = shotKey(song, seed, shotIndex, start, size, move, subjects.map { it.id })

/**
 * The ratings collected so far, kept as one JSON object per line so a session that ends abruptly loses nothing.
 */
class RatingStore(val file: File) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    /**
     * Every rating saved so far, oldest first. Ratings that have ended up on one line, as when an editor drops the
     * file's last line break, are still read. Anything that isn't a rating is skipped.
     */
    fun load(): List<ShotRating> = if (!file.isFile) {
        emptyList()
    } else {
        file.readLines().flatMap(::objectsIn).mapNotNull {
            runCatching { json.decodeFromString(ShotRating.serializer(), it) }.getOrNull()
        }
    }

    /** Saves [rating] at the end of the file, on a line of its own. */
    fun append(rating: ShotRating) {
        file.parentFile?.mkdirs()
        val needsBreak = file.isFile && file.length() > 0 && !file.readText().endsWith("\n")
        file.appendText((if (needsBreak) "\n" else "") + json.encodeToString(ShotRating.serializer(), rating) + "\n")
    }

    /** The top-level JSON objects in [line], in order. */
    private fun objectsIn(line: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false
        line.forEachIndexed { i, c ->
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }

                c == '}' && depth > 0 -> {
                    depth--
                    if (depth == 0) objects += line.substring(start, i + 1)
                }
            }
        }
        return objects
    }
}
