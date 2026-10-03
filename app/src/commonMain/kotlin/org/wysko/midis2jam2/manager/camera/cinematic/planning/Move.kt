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

/** The lowest the camera may look from, in degrees of pitch. */
private const val MIN_RIG_PITCH = -15f

/** The highest the camera may look from, in degrees of pitch. */
private const val MAX_RIG_PITCH = 80f

/**
 * Where the camera rig is at one instant of a shot, relative to the shot's framing.
 *
 * @property yaw The direction the camera looks from, in degrees.
 * @property pitch How far it looks down, in degrees.
 * @property distanceScale How far back it stands, as a multiple of the snug framing distance.
 * @property lateral How far it has slid sideways, as a multiple of its distance from the subject. The camera turns
 * to keep the subject on its anchor.
 * @property vertical How far it has risen, as a multiple of its distance from the subject. The camera tilts to keep
 * the subject on its anchor.
 * @property holdSize Whether the lens zooms to cancel out the change in distance, keeping the subject the same size.
 */
data class RigFrame(
    val yaw: Float,
    val pitch: Float,
    val distanceScale: Float = 1f,
    val lateral: Float = 0f,
    val vertical: Float = 0f,
    val holdSize: Boolean = false,
)

/**
 * How one part of a move travels.
 *
 * @property rate How fast it travels, in its own units per second.
 * @property reach How far it may travel either side of the shot's framing, however long the shot.
 */
private class Travel(val rate: Float, val reach: Float) {
    /** How far it has travelled, signed, after [motion] seconds of motion either side of the shot's middle. */
    fun at(motion: Float, halfDuration: Float): Float {
        // A long shot covers more ground, up to its reach; a short one simply moves less. Never faster.
        val speed = if (halfDuration > 0f) minOf(rate, reach / halfDuration) else rate
        return speed * motion
    }
}

/**
 * The ways the camera can move during a shot, named after the rigs that make them on a film set.
 *
 * Every move travels at a steady, gentle speed, with the shot's planned framing at its middle. A cut lands on a
 * camera that's already moving, and the next cut takes it away mid-move, the way a film editor cuts into and out of
 * a camera move. Only the last shot of a film eases to a stop.
 */
enum class Move {
    /** Locked off on a tripod. */
    Static,

    /** A dolly towards the subject: building intensity. */
    PushIn,

    /** A dolly away from the subject: release, or a reveal of what's around it. */
    PullOut,

    /** Sliding left along a track parallel to the stage, with parallax past the instruments in front. */
    TrackLeft,

    /** Sliding right along a track parallel to the stage. */
    TrackRight,

    /** A jib rising in an arc and looking down: a reveal. */
    CraneUp,

    /** A jib descending in an arc to settle on the subject. */
    CraneDown,

    /** Circling the subject to the left. */
    ArcLeft,

    /** Circling the subject to the right. */
    ArcRight,

    /** Rising straight up, without arcing. */
    Pedestal,

    /** High above, looking almost straight down, drifting slightly. */
    Overhead,

    /** Dollying in while zooming out, so the subject holds its size while the background warps. For big moments. */
    DollyZoom,

    /** A long, sweeping crane across the front of the whole stage. For openings and endings. */
    StageSweep,
    ;

    /**
     * Where the rig is [elapsed] seconds into a shot lasting [length] seconds, framed from [yaw] and [pitch].
     *
     * @param settles Whether the camera eases to a stop by the end of the shot, as the last shot of a film does.
     * Otherwise it moves at a steady speed throughout.
     */
    fun rig(yaw: Float, pitch: Float, elapsed: Double, length: Double, settles: Boolean = false): RigFrame {
        val duration = length.coerceAtLeast(1e-3).toFloat()
        val t = elapsed.toFloat().coerceIn(0f, duration)

        // Seconds of motion either side of the middle of the move. A settling move starts at full speed and slows
        // to a stop, so it covers half the ground of a steady one.
        val (motion, half) = if (settles) {
            (t - t * t / (2 * duration) - duration / 4) to duration / 4
        } else {
            (t - duration / 2) to duration / 2
        }
        fun Travel.go(): Float = at(motion, half)

        return when (this) {
            Static -> RigFrame(yaw, pitch)
            PushIn -> RigFrame(yaw, pitch, distanceScale = 1.1f - DOLLY.go())
            PullOut -> RigFrame(yaw, pitch, distanceScale = 1.1f + DOLLY.go())
            TrackLeft -> RigFrame(yaw, pitch, lateral = -TRACK.go())
            TrackRight -> RigFrame(yaw, pitch, lateral = TRACK.go())
            CraneUp -> CRANE.go().let { RigFrame(yaw, pitch + 6f + it, distanceScale = 1.07f + 0.004f * it) }
            CraneDown -> CRANE.go().let { RigFrame(yaw, pitch + 8f - it, distanceScale = 1.07f - 0.004f * it) }
            ArcLeft -> RigFrame(yaw - ARC.go(), pitch)
            ArcRight -> RigFrame(yaw + ARC.go(), pitch)
            Pedestal -> RigFrame(yaw, pitch, vertical = PEDESTAL.go())
            Overhead -> RigFrame(yaw + DRIFT.go(), 72f, distanceScale = 1.05f)
            DollyZoom -> RigFrame(yaw, pitch, distanceScale = 1.2f - ZOOM_DOLLY.go(), holdSize = true)
            StageSweep -> RigFrame(yaw + SWEEP.go(), 18f + SWEEP_RISE.go())
        }.let { it.copy(pitch = it.pitch.coerceIn(MIN_RIG_PITCH, MAX_RIG_PITCH)) }
    }

    private companion object {
        /** Dollying: 2.5% of the framing distance a second, up to 18% either way. */
        val DOLLY = Travel(0.025f, 0.18f)

        /** Tracking: 3% of the distance to the subject a second, up to a quarter of it either way. */
        val TRACK = Travel(0.03f, 0.25f)

        /** Craning: 2.5 degrees a second, up to 15 either way. */
        val CRANE = Travel(2.5f, 15f)

        /** Arcing: 2.2 degrees a second, up to 18 either way. */
        val ARC = Travel(2.2f, 18f)

        /** Rising on a pedestal: 2.5% of the distance to the subject a second, up to 15% either way. */
        val PEDESTAL = Travel(0.025f, 0.15f)

        /** Drifting overhead: 1.2 degrees a second, up to 8 either way. */
        val DRIFT = Travel(1.2f, 8f)

        /** The dolly-zoom's travel, which has to be big enough to see: 10% a second, up to 40% either way. */
        val ZOOM_DOLLY = Travel(0.1f, 0.4f)

        /** Sweeping across the stage: 2.5 degrees a second, up to 30 either way, rising 1 degree a second. */
        val SWEEP = Travel(2.5f, 30f)
        val SWEEP_RISE = Travel(1f, 8f)
    }
}
