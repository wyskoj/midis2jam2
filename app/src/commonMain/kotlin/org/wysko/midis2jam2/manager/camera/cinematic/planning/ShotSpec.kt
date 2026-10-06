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

package org.wysko.midis2jam2.manager.camera.cinematic.planning

/**
 * How much of the frame a shot's subject fills.
 *
 * @property fill How much of the frame, from the subject's anchor to the nearer edge, the subject fills. Above 1,
 * the frame crops the subject.
 */
enum class ShotSize(val fill: Float) {
    /** Everything on stage. */
    Establishing(1f),

    /** A group of players, filling the frame. */
    Wide(0.9f),

    /** The subject and a little of its surroundings. */
    Medium(0.62f),

    /** The subject filling the frame. */
    CloseUp(0.88f),

    /** In close on the subject, only just cropped by the frame. */
    Insert(1.05f),
}

/**
 * The lens a shot is filmed with, as a multiple of the user's chosen field of view.
 */
enum class LensChoice(val fovScale: Float) {
    /** Wider than normal: closer to the subject, with exaggerated depth. */
    Wide(1.25f),

    /** The user's field of view. */
    Normal(1f),

    /** A long lens: further back, with the background compressed behind the subject. */
    Long(0.6f),
}

/** How the camera gets into a shot. */
enum class Transition {
    /** An instant change of shot. */
    Cut,

    /**
     * A quick, smooth turn from the shot before, when the camera need only swing round a little to find its new
     * subject. The camera decides when the shot begins whether the two views are close enough; if not, it cuts.
     */
    Whip,

    /**
     * A gentle move from the shot before, by way of a shot with both players in frame. The camera decides when the
     * shot begins whether the two fit in one frame; if not, it cuts.
     */
    Glide,
}

/**
 * Everything about a shot except when it happens.
 *
 * @property subjects The subjects in frame, by id. Empty means everything on stage.
 * @property size How much of the frame the subjects fill.
 * @property yaw How far round from straight on the camera looks from, in degrees. Positive swings towards +X.
 * @property pitch How far above the subjects the camera looks down from, in degrees.
 * @property lens The lens.
 * @property move How the camera moves during the shot.
 */
data class ShotSpec(
    val subjects: List<Int>,
    val size: ShotSize,
    val yaw: Float,
    val pitch: Float,
    val lens: LensChoice,
    val move: Move,
)

/**
 * A shot in the plan.
 *
 * @property start When the shot begins, in seconds.
 * @property end When the next shot takes over, in seconds.
 * @property spec What the shot looks like.
 * @property reason Why the shot was chosen, for the debug readout.
 * @property settles Whether the camera eases to a stop by the end of the shot, as it does at the end of the film.
 * @property transition How the camera gets into the shot.
 */
data class PlannedShot(
    val start: Double,
    val end: Double,
    val spec: ShotSpec,
    val reason: String,
    val settles: Boolean = false,
    val transition: Transition = Transition.Cut,
) {
    /** How long the shot lasts, in seconds. */
    val length: Double get() = end - start

    /** Where the camera rig is at [time]. */
    fun rig(time: Double, yaw: Float = spec.yaw, pitch: Float = spec.pitch): RigFrame =
        spec.move.rig(yaw, pitch, time - start, length, settles)
}

/**
 * The whole edit: one shot after another, with no gaps, from before the song starts to forever after it ends.
 */
class ShotPlan(val shots: List<PlannedShot>, val seed: Long) {
    init {
        require(shots.isNotEmpty()) { "A plan needs at least one shot." }
    }

    /** The index of the shot playing at [time]. */
    fun indexAt(time: Double): Int {
        var low = 0
        var high = shots.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (shots[mid].start <= time) low = mid else high = mid - 1
        }
        return low
    }

    /** The shot playing at [time]. */
    fun at(time: Double): PlannedShot = shots[indexAt(time)]
}
